package com.astramesh.transport.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import com.astramesh.transport.PeerEndpoint
import com.astramesh.transport.TransportKind

/**
 * Wraps the Android BLE scanner (docs/architecture.md §8).
 *
 * Scans with no OS-level [ScanFilter] and filters manually in [toEndpoint] by checking for the
 * AstraMesh service data UUID -- see the comment in [start] for why a `ScanFilter.setServiceUuid`
 * filter does not work with this advertiser. Reports each discovered node as a [PeerEndpoint]
 * via [onPeer]. Callers must hold BLUETOOTH_SCAN (API 31+) / location (API < 31) before [start].
 */
@SuppressLint("MissingPermission")
class BleScanner(private val context: Context) {

    private val scanner: BluetoothLeScanner?
        get() {
            val manager = context.getSystemService(BluetoothManager::class.java)
            return manager?.adapter?.takeIf { it.isEnabled }?.bluetoothLeScanner
        }

    private var callback: ScanCallback? = null

    fun isAvailable(): Boolean = scanner != null

    fun start(onPeer: (PeerEndpoint) -> Unit, onError: (String) -> Unit = {}) {
        val sc = scanner ?: run {
            Log.w(TAG, "BLE scanner unavailable")
            onError("BLE scanner unavailable (Bluetooth off or unsupported)")
            return
        }
        if (callback != null) return

        // No ScanFilter here. ScanFilter.setServiceUuid() matches ONLY the "Complete List of
        // Service UUIDs" AD structure -- exactly what BleAdvertiser used to send via
        // addServiceUuid(). That call was removed from the advertiser to fix a legacy
        // 31-byte advertisement overflow (see BleAdvertiser's byte-budget comment): our
        // advertisements now carry ONLY a Service Data AD structure, no Service UUID AD
        // structure at all. A scan filter asking for a Service UUID AD structure that our own
        // advertisement never sends matches nothing, ever -- which silently zeroed out every
        // scan result regardless of permissions, Location toggle, or proximity. Filtering is
        // done manually in toEndpoint() below (only returns non-null when our service data UUID
        // is present), which is what actually enforces "this is an AstraMesh device."
        val filters = emptyList<ScanFilter>()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val cb = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                result.toEndpoint()?.let(onPeer)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach { it.toEndpoint()?.let(onPeer) }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.w(TAG, "Scan failed: $errorCode")
                onError("Scan failed (error code $errorCode)")
            }
        }
        callback = cb
        runCatching { sc.startScan(filters, settings, cb) }
            .onFailure {
                Log.w(TAG, "startScan threw", it)
                callback = null
                onError("Scan start threw: ${it.message}")
            }
    }

    fun stop() {
        val cb = callback ?: return
        runCatching { scanner?.stopScan(cb) }
        callback = null
    }

    private fun ScanResult.toEndpoint(): PeerEndpoint? {
        val serviceData = scanRecord
            ?.getServiceData(ParcelUuid(BleConstants.SERVICE_UUID))
            ?: return null
        val nodeId = String(serviceData, Charsets.UTF_8).ifBlank { return null }
        return PeerEndpoint(
            nodeId = nodeId,
            address = device.address,
            transport = TransportKind.BLE,
            signalStrength = rssi,
        )
    }

    companion object {
        private const val TAG = "BleScanner"
    }
}
