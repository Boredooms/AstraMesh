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
     * GATT characteristic the server side INDICATES on, to push a packet back to the
     * connected client over the SAME connection. This is what makes one GATT connection fully
     * bidirectional: whichever node happened to connect (e.g. to deliver a HELLO) does not
     * need the other side to open a second, independent connection back just to reply (that
     * second connection depending on independent scan/MAC-discovery on the replying side was
     * the root cause of one-sided handshakes -- see docs/architecture.md §9 and
     * BleTransport.send()'s doc comment).
     *
     * Deliberately an INDICATE characteristic, not NOTIFY. A GATT notification is fire-and-
     * forget at the application layer -- the sender's onNotificationSent callback only
     * confirms the local Bluetooth stack accepted the send, NOT that the remote device
     * actually received it (https://docs.silabs.com/bluetooth/3.2/bluetooth-general-gatt-protocol/:
     * "Notifications are unacknowledged, while indications are acknowledged. Notifications are
     * therefore faster but less reliable."). Since packets are fragmented into several chunks,
     * an unacknowledged notify means later chunks can be sent (and the whole packet reported
     * as "sent") before the peer has even received earlier ones, or lost entirely with no
     * signal to the sender -- which is exactly the "sometimes relayed/received, sometimes not,
     * even while showing Connected" behavior this fixes. GATT indications require the
     * receiving device's Bluetooth stack to send an ATT-level confirmation before the next
     * indication is allowed to send, giving real per-chunk delivery confirmation for free.
     */
    val NOTIFY_CHARACTERISTIC_UUID: UUID = UUID.fromString("a57a3e02-9b1e-4c2f-8f6d-0a11e5ab5100")

    /** Client Characteristic Configuration Descriptor for enabling notifications. */
    val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    /** Manufacturer id used to tag advertisements (arbitrary, unregistered). */
    const val MANUFACTURER_ID: Int = 0xA57A

    /** Conservative BLE-friendly MTU target for packet fragmentation (docs/architecture.md §9). */
    const val TARGET_MTU: Int = 512
}
