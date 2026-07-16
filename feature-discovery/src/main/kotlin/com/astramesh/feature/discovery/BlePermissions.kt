package com.astramesh.feature.discovery

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat

/**
 * Runtime permissions required to advertise + scan over Bluetooth LE (docs/architecture.md §8).
 *
 * Declaring these in the manifest is not enough on API 31+ (Android 12) — `BLUETOOTH_SCAN`,
 * `BLUETOOTH_ADVERTISE`, and `BLUETOOTH_CONNECT` are dangerous runtime permissions there, and
 * without an explicit grant `BluetoothLeAdvertiser.startAdvertising()` /
 * `BluetoothLeScanner.startScan()` throw `SecurityException`, which the transport layer was
 * swallowing silently (see the "two nearby devices see 0 peers forever" bug this fixes).
 */
object BlePermissions {

    /** The permission set to request for the running device's API level. */
    fun required(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
        } else {
            // Pre-S: BLE scanning requires (fine) location; BLUETOOTH/BLUETOOTH_ADMIN are
            // normal permissions granted automatically at install time.
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    /** True if every permission in [required] is currently granted. */
    fun allGranted(context: Context): Boolean =
        required().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    /**
     * True if system-wide Location Services must be switched ON (the Quick Settings toggle,
     * not the app permission) for BLE scan results to be delivered at all on this device.
     *
     * The manifest declares `BLUETOOTH_SCAN` with `neverForLocation`, which removes this
     * requirement entirely on API 31+ (Android 12+) — AstraMesh never derives physical
     * location from scan results. Pre-31, there is no such flag: the OS still gates delivery
     * of any BLE scan result on the system Location toggle being on, independently of whether
     * `ACCESS_FINE_LOCATION` itself is granted. This is the exact "permission granted,
     * Bluetooth on, scan runs, still 0 peers" failure mode on older devices/OS versions.
     */
    fun locationServicesRequiredButOff(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return false
        val manager = context.getSystemService(LocationManager::class.java) ?: return false
        return !LocationManagerCompat.isLocationEnabled(manager)
    }
}
