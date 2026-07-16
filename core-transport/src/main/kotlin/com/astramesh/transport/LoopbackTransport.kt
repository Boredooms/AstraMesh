package com.astramesh.transport

import com.astramesh.protocol.Packet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * In-process transport used for tests, previews, and single-device demos.
 *
 * Multiple [LoopbackTransport] instances sharing the same [Bus] behave like nearby nodes: a
 * packet sent from one is delivered to the addressed peer's event stream. This is NOT a
 * placeholder for BLE — it is a real, deterministic transport that lets the mesh run
 * end-to-end without Bluetooth hardware (docs/protocol.md §7 "transport-agnostic").
 */
class LoopbackTransport(
    private val bus: Bus,
) : Transport {

    override val kind: TransportKind = TransportKind.LOOPBACK

    private val _events = MutableSharedFlow<TransportEvent>(extraBufferCapacity = 64)
    override val events: Flow<TransportEvent> = _events.asSharedFlow()

    private var selfId: String? = null

    override fun isAvailable(): Boolean = true

    override suspend fun start(selfNodeId: String) {
        selfId = selfNodeId
        bus.register(selfNodeId, this)
        // Announce existing peers to this node and this node to peers.
        bus.endpointsExcept(selfNodeId).forEach {
            _events.emit(TransportEvent.PeerDiscovered(it))
        }
        bus.announce(selfNodeId)
    }

    override suspend fun stop() {
        selfId?.let { bus.unregister(it) }
        selfId = null
    }

    override suspend fun send(packet: Packet, endpoint: PeerEndpoint): Boolean {
        val target = bus.transportFor(endpoint.nodeId) ?: return false
        val from = PeerEndpoint(
            nodeId = selfId ?: return false,
            address = selfId!!,
            transport = TransportKind.LOOPBACK,
        )
        target.deliver(packet, from)
        return true
    }

    override fun knownEndpoints(): List<PeerEndpoint> =
        selfId?.let { bus.endpointsExcept(it) } ?: emptyList()

    internal suspend fun deliver(packet: Packet, from: PeerEndpoint) {
        _events.emit(TransportEvent.PacketReceived(packet, from))
    }

    internal suspend fun onPeerAppeared(endpoint: PeerEndpoint) {
        _events.emit(TransportEvent.PeerDiscovered(endpoint))
    }

    /** Shared rendezvous point simulating the local radio neighborhood. */
    class Bus {
        private val nodes = ConcurrentHashMap<String, LoopbackTransport>()

        fun register(nodeId: String, transport: LoopbackTransport) {
            nodes[nodeId] = transport
        }

        fun unregister(nodeId: String) {
            nodes.remove(nodeId)
        }

        fun transportFor(nodeId: String): LoopbackTransport? = nodes[nodeId]

        fun endpointsExcept(nodeId: String): List<PeerEndpoint> =
            nodes.keys.filter { it != nodeId }.map {
                PeerEndpoint(nodeId = it, address = it, transport = TransportKind.LOOPBACK)
            }

        suspend fun announce(nodeId: String) {
            val self = PeerEndpoint(nodeId = nodeId, address = nodeId, transport = TransportKind.LOOPBACK)
            nodes.filterKeys { it != nodeId }.values.forEach { it.onPeerAppeared(self) }
        }
    }
}
