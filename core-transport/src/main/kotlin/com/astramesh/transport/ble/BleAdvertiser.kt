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

    /** Starts advertising this [nodeId]. The id is truncated to fit the advertisement budget. */
    fun start(nodeId: String) {
        val adv = advertiser ?: run {
            Log.w(TAG, "BLE advertiser unavailable")
            return
        }
        if (callback != null) return // already advertising

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(true)
            .build()

        // Node id shortened to keep the advertisement within the 31-byte limit.
        val shortId = nodeId.take(SERVICE_DATA_MAX_BYTES).toByteArray(Charsets.UTF_8)
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID))
            .addServiceData(ParcelUuid(BleConstants.SERVICE_UUID), shortId)
            .build()

        val cb = object : AdvertiseCallback() {
            override fun onStartFailure(errorCode: Int) {
                Log.w(TAG, "Advertise start failed: $errorCode")
                callback = null
            }
        }
        callback = cb
        runCatching { adv.startAdvertising(settings, data, cb) }
            .onFailure {
                Log.w(TAG, "startAdvertising threw", it)
                callback = null
            }
    }

    fun stop() {
        val cb = callback ?: return
        runCatching { advertiser?.stopAdvertising(cb) }
        callback = null
    }

    companion object {
        private const val TAG = "BleAdvertiser"
        private const val SERVICE_DATA_MAX_BYTES = 20
    }
}
