package com.astramesh.transport

import com.astramesh.protocol.Packet
import kotlinx.coroutines.flow.Flow

/**
 * Transport abstraction contract (docs/protocol.md §7, docs/architecture.md §9).
 *
 * The routing layer talks ONLY to this interface, never to device-specific APIs like the
 * Android Bluetooth stack. This keeps routing pure and lets the app swap or combine transports
 * (BLE, Wi-Fi Direct, desktop relay) without touching routing logic.
 *
 * Every backend must support: start/stop, send a packet to a peer, and surface a stream of
 * [TransportEvent]s (discovery, incoming packets, link changes, errors).
 */
interface Transport {

    val kind: TransportKind

    /** A hot stream of transport events. Collectors receive discovery + incoming packets. */
    val events: Flow<TransportEvent>

    /** True if this transport is currently available on the device (hardware/permissions). */
    fun isAvailable(): Boolean

    /** Begin advertising this node and scanning for peers. Idempotent. */
    suspend fun start(selfNodeId: String)

    /** Stop advertising/scanning and tear down links. Idempotent. */
    suspend fun stop()

    /**
     * Send [packet] to the peer identified by [endpoint].
     * @return true if handed to the link successfully (not a delivery guarantee).
     */
    suspend fun send(packet: Packet, endpoint: PeerEndpoint): Boolean

    /** Currently reachable endpoints as last known by this transport. */
    fun knownEndpoints(): List<PeerEndpoint>
}
