package com.astramesh.transport.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import android.util.Log

/**
 * Wraps the Android BLE advertiser (docs/architecture.md §8, docs/protocol.md §9.1).
 *
 * Advertises the AstraMesh service UUID plus a short node id in the service data so nearby
 * scanners can recognize this node. Callers must hold BLUETOOTH_ADVERTISE (API 31+) before
 * calling [start]; availability is checked defensively.
 */
@SuppressLint("MissingPermission")
class BleAdvertiser(private val context: Context) {

    private val advertiser: BluetoothLeAdvertiser?
        get() {
            val manager = context.getSystemService(BluetoothManager::class.java)
            return manager?.adapter?.takeIf { it.isEnabled }?.bluetoothLeAdvertiser
        }

    private var callback: AdvertiseCallback? = null

    fun isAvailable(): Boolean = advertiser != null

    /**
     * Starts advertising this [nodeId]. [onError] is invoked if the OS rejects the request
     * (e.g. missing runtime permission, oversized payload, adapter busy) so callers can
     * surface the failure instead of the mesh silently never becoming reachable.
     */
    fun start(nodeId: String, onError: (String) -> Unit = {}) {
        val adv = advertiser ?: run {
            Log.w(TAG, "BLE advertiser unavailable")
            onError("BLE advertiser unavailable (Bluetooth off or unsupported)")
            return
        }
        if (callback != null) return // already advertising

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(true)
            .build()

        // Legacy BLE advertisements are hard-capped at 31 bytes total. A custom 128-bit
        // service UUID alone costs 18 bytes (2-byte AD header + 16-byte UUID), plus another
        // 18 bytes if it's *also* carried in a Service Data AD structure, plus the flags AD
        // structure (3 bytes) Android adds automatically. addServiceUuid() + addServiceData()
        // with the SAME UUID double-counts it and overflows the packet -- startAdvertising()
        // then fails silently with ADVERTISE_FAILED_DATA_TOO_LARGE (error code 1), which is
        // exactly why two phones running this app could never see each other: neither side's
        // radio ever actually started advertising. Service Data's own UUID already identifies
        // the service to scanners (see BleScanner.toEndpoint, which reads it directly), so
        // addServiceUuid() here is redundant -- drop it and keep only Service Data.
        //
        // Budget: 3 (flags) + 2 (service data header) + 16 (UUID) = 21 bytes fixed overhead,
        // leaving SERVICE_DATA_MAX_BYTES (10) for the node id payload out of the 31-byte
        // packet. nodeId is generated at exactly this length (DataStoreNodeIdentity), so this
        // is a safety bound, not a silent truncation that would desync the advertised id from
        // the id used as the addressing key elsewhere in the mesh (packet sender/receiver ids).
        val idBytes = nodeId.toByteArray(Charsets.UTF_8)
        check(idBytes.size <= SERVICE_DATA_MAX_BYTES) {
            "nodeId '$nodeId' (${idBytes.size} bytes) exceeds the BLE advertisement budget " +
                "of $SERVICE_DATA_MAX_BYTES bytes; truncating it would desync discovery from " +
                "routing since nodeId is used as the packet addressing key"
        }
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceData(ParcelUuid(BleConstants.SERVICE_UUID), idBytes)
            .build()

        val cb = object : AdvertiseCallback() {
            override fun onStartFailure(errorCode: Int) {
                Log.w(TAG, "Advertise start failed: $errorCode")
                callback = null
                onError("Advertise failed to start (error code $errorCode)")
            }
        }
        callback = cb
        runCatching { adv.startAdvertising(settings, data, cb) }
            .onFailure {
                Log.w(TAG, "startAdvertising threw", it)
                callback = null
                onError("Advertise start threw: ${it.message}")
            }
    }

    fun stop() {
        val cb = callback ?: return
        runCatching { advertiser?.stopAdvertising(cb) }
        callback = null
    }

    companion object {
        private const val TAG = "BleAdvertiser"

        /**
         * Max bytes available for the node id inside Service Data, given the 31-byte legacy
         * advertisement limit: 31 - 3 (flags) - 2 (service data AD header) - 16 (128-bit UUID).
         */
        private const val SERVICE_DATA_MAX_BYTES = 10
    }
}
