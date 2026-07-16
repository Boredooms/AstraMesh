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

    /**
     * GATT characteristic a connecting client WRITES to, to deliver a packet to whichever
     * node is running the GATT server for this connection.
     */
    val PACKET_CHARACTERISTIC_UUID: UUID = UUID.fromString("a57a3e01-9b1e-4c2f-8f6d-0a11e5ab5100")

    /**
     * GATT characteristic the server side NOTIFIES on, to push a packet back to the connected
     * client over the SAME connection. This is what makes one GATT connection fully
     * bidirectional: whichever node happened to connect (e.g. to deliver a HELLO) does not
     * need the other side to open a second, independent connection back just to reply (that
     * second connection depending on independent scan/MAC-discovery on the replying side was
     * the root cause of one-sided handshakes -- see docs/architecture.md §9 and
     * BleTransport.send()'s doc comment).
     */
    val NOTIFY_CHARACTERISTIC_UUID: UUID = UUID.fromString("a57a3e02-9b1e-4c2f-8f6d-0a11e5ab5100")

    /** Client Characteristic Configuration Descriptor for enabling notifications. */
    val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    /** Manufacturer id used to tag advertisements (arbitrary, unregistered). */
    const val MANUFACTURER_ID: Int = 0xA57A

    /** Conservative BLE-friendly MTU target for packet fragmentation (docs/architecture.md §9). */
    const val TARGET_MTU: Int = 512
}
