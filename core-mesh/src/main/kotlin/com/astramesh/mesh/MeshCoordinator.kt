package com.astramesh.mesh

import com.astramesh.domain.identity.NodeIdentityProvider
import com.astramesh.domain.model.DeliveryState
import com.astramesh.domain.model.Message
import com.astramesh.domain.repository.MessageRepository
import com.astramesh.domain.repository.PeerRepository
import com.astramesh.protocol.Packet
import com.astramesh.protocol.PacketType
import com.astramesh.protocol.Priority
import com.astramesh.protocol.payload.ChatPayload
import com.astramesh.routing.RoutingContext
import com.astramesh.routing.RoutingDecision
import com.astramesh.routing.RoutingEngine
import com.astramesh.security.MessageCipher
import com.astramesh.transport.PeerEndpoint
import com.astramesh.transport.Transport
import com.astramesh.transport.TransportEvent
import com.astramesh.transport.TransportKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.UUID

/**
 * The heart of the mesh: ties transport, routing, security and persistence together
 * (docs/architecture.md §3, §14; docs/workflow.md §7–9).
 *
 * Sending a chat message: persist as PENDING → encrypt payload → build packet → route → send.
 * Receiving a packet: route it (dedup/TTL) → deliver-locally (decrypt + persist + ACK),
 * forward/relay onward, or queue for store-and-forward.
 *
 * Depends only on interfaces + pure engines, so it is unit-testable on the JVM with a
 * LoopbackTransport (see MeshCoordinatorTest).
 *
 * @param clock injectable time source so tests are deterministic
 */
class MeshCoordinator(
    private val transport: Transport,
    private val routing: RoutingEngine,
    private val messages: MessageRepository,
    private val peers: PeerRepository,
    private val identity: NodeIdentityProvider,
    private val sessionKeys: SessionKeyManager,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {

    fun start(scope: CoroutineScope) {
        transport.events
            .onEach { event ->
                if (event is TransportEvent.PacketReceived) {
                    handleIncoming(event.packet, event.from)
                }
            }
            .launchIn(scope)
    }

    /**
     * Composes and sends a chat message to [toNodeId]. The message is persisted before it
     * leaves (persist-first, docs/workflow.md §13) and encrypted end to end.
     * @return the persisted [Message], or null if no session key is available for the peer.
     */
    suspend fun sendChat(toNodeId: String, text: String, replyToId: String? = null): Message? {
        val self = identity.nodeId()
        val now = clock()
        val packetId = UUID.randomUUID().toString()

        val message = Message(
            id = UUID.randomUUID().toString(),
            packetId = packetId,
            senderId = self,
            receiverId = toNodeId,
            text = text,
            timestamp = now,
            state = DeliveryState.PENDING,
            outgoing = true,
            replyToId = replyToId,
        )
        messages.upsert(message) // persist first

        val key = sessionKeyFor(toNodeId) ?: return message
        val sealed = MessageCipher.sealPayload(key, ChatPayload(text, replyToId))

        val packet = Packet(
            packetId = packetId,
            type = PacketType.CHAT,
            senderId = self,
            receiverId = toNodeId,
            timestamp = now,
            ttl = Packet.DEFAULT_TTL,
            priority = Priority.NORMAL,
            payload = sealed,
        )

        val sent = dispatch(packet)
        messages.updateState(packetId, if (sent) DeliveryState.SENT else DeliveryState.PENDING)
        return message.copy(state = if (sent) DeliveryState.SENT else DeliveryState.PENDING)
    }

    /** Processes an incoming packet through the routing engine and acts on the decision. */
    suspend fun handleIncoming(packet: Packet, from: PeerEndpoint) {
        val self = identity.nodeId()
        val context = RoutingContext(
            localNodeId = self,
            directPeers = directPeerIds(),
            relayPeers = relayPeerIds(),
            now = clock(),
        )
        when (val decision = routing.route(packet, context)) {
            is RoutingDecision.Drop -> Unit
            is RoutingDecision.DeliverLocally -> deliverLocally(decision.packet)
            is RoutingDecision.ForwardDirect -> sendTo(decision.packet, decision.nextHop)
            is RoutingDecision.Relay -> decision.neighbors.forEach { sendTo(decision.packet, it) }
            is RoutingDecision.Queue -> { /* stored implicitly; retried on next contact */ }
        }
    }
    private suspend fun deliverLocally(packet: Packet) {
        when (packet.type) {
            PacketType.CHAT -> persistIncomingChat(packet)
            else -> Unit // other types handled in later stages
        }
    }

    private suspend fun persistIncomingChat(packet: Packet) {
        val key = sessionKeyFor(packet.senderId) ?: return
        val payload = runCatching { MessageCipher.openPayload(key, packet.payload) }
            .getOrNull() as? ChatPayload ?: return

        messages.upsert(
            Message(
                id = UUID.randomUUID().toString(),
                packetId = packet.packetId,
                senderId = packet.senderId,
                receiverId = packet.receiverId,
                text = payload.text,
                timestamp = packet.timestamp,
                state = DeliveryState.DELIVERED,
                outgoing = false,
                hopCount = packet.hopCount,
                replyToId = payload.replyToPacketId,
            )
        )
    }

    /** Sends [packet] toward its destination via the best available route. */
    private suspend fun dispatch(packet: Packet): Boolean {
        val context = RoutingContext(
            localNodeId = identity.nodeId(),
            directPeers = directPeerIds(),
            relayPeers = relayPeerIds(),
            now = clock(),
        )
        return when (val decision = routing.route(packet, context)) {
            is RoutingDecision.ForwardDirect -> sendTo(decision.packet, decision.nextHop)
            is RoutingDecision.Relay ->
                decision.neighbors.map { sendTo(decision.packet, it) }.any { it }
            else -> false
        }
    }

    private suspend fun sendTo(packet: Packet, nodeId: String): Boolean {
        val endpoint = transport.knownEndpoints().firstOrNull { it.nodeId == nodeId }
            ?: PeerEndpoint(nodeId, nodeId, TransportKind.LOOPBACK)
        return transport.send(packet, endpoint)
    }

    private suspend fun sessionKeyFor(nodeId: String) =
        peers.getNode(nodeId)?.let { sessionKeys.keyFor(nodeId, it.publicKey) }

    /**
     * Direct/relay neighbors come from ACTIVE peer sessions (who this node is connected to),
     * NOT from every device the radio can hear. This is what lets a message relay hop-by-hop:
     * A whose only session is with B will route an A->C packet through B.
     */
    private suspend fun activePeers(): List<com.astramesh.domain.model.Peer> =
        peers.observePeers().first()

    private suspend fun directPeerIds(): Set<String> =
        activePeers().filter { it.isConnected }.map { it.nodeId }.toSet()

    private suspend fun relayPeerIds(): Set<String> =
        activePeers().filter { it.isConnected && it.node.supportsRelay }.map { it.nodeId }.toSet()
}
