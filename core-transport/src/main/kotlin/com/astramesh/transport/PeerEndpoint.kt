package com.astramesh.transport

import com.astramesh.protocol.Packet

/** A reachable peer endpoint discovered by a transport (docs/protocol.md §7). */
data class PeerEndpoint(
    val nodeId: String,
    val address: String,
    val transport: TransportKind,
    val signalStrength: Int? = null,
    val relaySupported: Boolean = true,
)

/** Which physical transport an endpoint or event came from. */
enum class TransportKind { BLE, WIFI_DIRECT, DESKTOP, LOOPBACK }

/** Events emitted by a transport as the mesh changes (docs/architecture.md §9). */
sealed interface TransportEvent {
    data class PeerDiscovered(val endpoint: PeerEndpoint) : TransportEvent
    data class PeerLost(val nodeId: String) : TransportEvent
    data class PacketReceived(val packet: Packet, val from: PeerEndpoint) : TransportEvent
    data class LinkStateChanged(val nodeId: String, val state: LinkState) : TransportEvent
    data class Error(val message: String, val cause: Throwable? = null) : TransportEvent
}

enum class LinkState { CONNECTING, CONNECTED, INTERRUPTED, DISCONNECTED }
