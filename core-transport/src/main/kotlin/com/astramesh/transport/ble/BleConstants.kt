package com.astramesh.transport.ble

import java.util.UUID

/**
 * Bluetooth LE constants for AstraMesh (docs/protocol.md §7, §9.1).
 *
 * A single service UUID identifies AstraMesh nodes during advertisement/scan. The node id is
 * carried in the advertisement's service data so peers can be recognized before a GATT
 * connection is opened.
 */
object BleConstants {

    /** Service UUID advertised by every AstraMesh node. */
    val SERVICE_UUID: UUID = UUID.fromString("a57a3e00-9b1e-4c2f-8f6d-0a11e5ab5100")

    /** GATT characteristic used to exchange packet bytes once connected. */
    val PACKET_CHARACTERISTIC_UUID: UUID = UUID.fromString("a57a3e01-9b1e-4c2f-8f6d-0a11e5ab5100")

    /** Client Characteristic Configuration Descriptor for enabling notifications. */
    val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    /** Manufacturer id used to tag advertisements (arbitrary, unregistered). */
    const val MANUFACTURER_ID: Int = 0xA57A

    /** Conservative BLE-friendly MTU target for packet fragmentation (docs/architecture.md §9). */
    const val TARGET_MTU: Int = 512
}
