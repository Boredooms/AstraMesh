package com.astramesh.mesh

import com.astramesh.domain.identity.NodeIdentityProvider
import com.astramesh.domain.model.Capability
import com.astramesh.domain.model.DeliveryState
import com.astramesh.domain.model.Message
import com.astramesh.domain.model.Node
import com.astramesh.domain.model.Peer
import com.astramesh.domain.model.PlatformType
import com.astramesh.domain.model.SessionState
import com.astramesh.domain.repository.MessageRepository
import com.astramesh.domain.repository.PeerRepository
import com.astramesh.domain.repository.RelayQueueRepository
import com.astramesh.protocol.Packet
import com.astramesh.protocol.PacketType
import com.astramesh.protocol.Priority
import com.astramesh.protocol.ProtocolJson
import com.astramesh.protocol.ProtocolVersion
import com.astramesh.protocol.payload.AckPayload
import com.astramesh.protocol.payload.ChatPayload
import com.astramesh.protocol.payload.HandshakePayload
import com.astramesh.routing.RoutingContext
import com.astramesh.routing.RoutingDecision
import com.astramesh.routing.RoutingEngine
import com.astramesh.security.KeyExchange
import com.astramesh.security.MessageCipher
import com.astramesh.transport.LinkState
import com.astramesh.transport.PeerEndpoint
import com.astramesh.transport.Transport
import com.astramesh.transport.TransportEvent
import com.astramesh.transport.TransportKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
    private val relayQueue: RelayQueueRepository? = null,
    private val counters: PacketCounters? = null,
    private val clock: () -> Long = { System.currentTimeMillis() },
    /** Bounded retry budget before a message/relay packet is marked FAILED/dropped. */
    private val maxRetries: Int = 5,
) {
    private companion object {
        /** How often the safety-net retry sweep runs (see [start]). */
        const val RETRY_SWEEP_INTERVAL_MS = 10_000L
    }


    /** NodeIds the local user has expressed intent to talk to but that aren't ACTIVE yet. */
    private val pendingHandshakes = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    fun start(scope: CoroutineScope) {
        transport.events
            .onEach { event ->
                when (event) {
                    is TransportEvent.PacketReceived -> handleIncoming(event.packet, event.from)
                    // NOTE: radio-level discovery does NOT auto-handshake. A transport reporting
                    // a peer as reachable (e.g. same BLE range) is not the same as this node
                    // having chosen to talk to them — the mesh graph is defined by ACTIVE
                    // sessions, established explicitly via connectTo(), not by radio proximity.
                    // This also matters for correctness: LoopbackTransport reports every node on
                    // its Bus as mutually "discovered", so auto-handshaking here would create a
                    // phantom direct session in multi-hop scenarios where a node should only be
                    // reachable through a relay. We DO use rediscovery to retry queued work,
                    // since that's a real reconnect signal for already-known peers/relays.
                    is TransportEvent.PeerDiscovered -> onPeerReachable(event.endpoint.nodeId)
                    // A GATT link dropped or failed to connect: reflect this in the peer's
                    // session state so the UI can show "offline" instead of a stale ACTIVE
                    // chip from the last successful handshake. Does not remove the peer --
                    // PeerLost (radio-level, out of range) is the signal for that.
                    is TransportEvent.LinkStateChanged -> onLinkStateChanged(event)
                    else -> Unit
                }
            }
            .launchIn(scope)

        // A peer already known (already DISCOVERED/INTERRUPTED, not freshly (re)discovered)
        // never triggers onPeerReachable, so a handshake or send that failed on its very first
        // attempt (e.g. a transient BLE connect failure) would otherwise sit stuck forever with
        // no automatic recovery until the user manually left and reopened the chat. This loop
        // is the safety net: it periodically retries pending chat messages, queued relay
        // packets, and any handshake the user asked for that hasn't completed yet, regardless
        // of whether a fresh discovery event happened to fire.
        scope.launch {
            while (isActive) {
                delay(RETRY_SWEEP_INTERVAL_MS)
                retryPendingMessages()
                retryRelayQueue()
                retryHandshakes()
            }
        }
    }

    /**
     * Explicitly initiates a secure session with [toNodeId] (docs/protocol.md §11 HELLO).
     * Call this when the user chooses to start a conversation with a peer — e.g. from the
     * peer picker or when opening a thread with someone not yet ACTIVE. No-ops if a session
     * is already active.
     */
    suspend fun connectTo(toNodeId: String) {
        val alreadyActive = peers.observePeers().first().any { it.nodeId == toNodeId && it.isConnected }
        if (!alreadyActive) {
            pendingHandshakes.add(toNodeId)
            sendHandshake(toNodeId)
        }
    }

    /**
     * Retries [connectTo] for any peer the user asked to talk to that hasn't reached
     * [SessionState.ACTIVE] yet. Covers the case where the very first handshake send attempt
     * failed at the transport level (e.g. a one-off BLE connect failure) and the peer was
     * already known, so no new [TransportEvent.PeerDiscovered] ever arrives to trigger a
     * retry via [onPeerReachable].
     */
    private suspend fun retryHandshakes() {
        if (pendingHandshakes.isEmpty()) return
        val activeIds = peers.observePeers().first().filter { it.isConnected }.map { it.nodeId }.toSet()
        pendingHandshakes.removeAll(activeIds)
        pendingHandshakes.forEach { nodeId -> sendHandshake(nodeId) }
    }

    /**
     * Sends this node's identity + public key to [toNodeId] to begin a secure session
     * (docs/protocol.md §11 HELLO). Handshake packets are necessarily unencrypted — there is
     * no session key yet — but carry no message content, only public identity material.
     */
    private suspend fun sendHandshake(toNodeId: String) {
        val self = identity.localNode()
        val packet = Packet(
            packetId = UUID.randomUUID().toString(),
            type = PacketType.HANDSHAKE,
            senderId = self.nodeId,
            receiverId = toNodeId,
            timestamp = clock(),
            ttl = 1, // handshakes are direct, single-hop only — never relayed
            priority = Priority.NORMAL,
            payload = ProtocolJson.encodePayload(
                HandshakePayload(
                    nodeId = self.nodeId,
                    publicKey = self.publicKey,
                    nonce = UUID.randomUUID().toString(),
                    protocolVersion = ProtocolVersion.CURRENT,
                    capabilities = self.capabilities.map { it.name },
                )
            ),
        )
        sendTo(packet, toNodeId)
        counters?.onHandshakeSent()
    }

    /** A peer's HELLO arrived: record their identity, mark the session ACTIVE, and reply. */
    private suspend fun handleHandshake(packet: Packet) {
        val payload = runCatching { ProtocolJson.decodePayload(packet.payload) }
            .getOrNull() as? HandshakePayload ?: return

        val wasAlreadyActive = peers.observePeers().first()
            .any { it.nodeId == payload.nodeId && it.isConnected }

        val node = Node(
            nodeId = payload.nodeId,
            deviceName = payload.nodeId.take(8),
            platformType = PlatformType.ANDROID,
            publicKey = payload.publicKey,
            keyFingerprint = KeyExchange.fingerprintOf(payload.publicKey),
            capabilities = payload.capabilities.mapNotNull { name ->
                runCatching { Capability.valueOf(name) }.getOrNull()
            }.toSet(),
            lastSeen = clock(),
        )
        peers.upsertPeer(Peer(node = node, sessionState = SessionState.ACTIVE, signalStrength = null, lastContact = clock()))
        pendingHandshakes.remove(payload.nodeId)
        counters?.onHandshakeReceived()

        // Reply with our own handshake so the initiator also reaches ACTIVE (HELLO_ACK).
        if (!wasAlreadyActive) {
            sendHandshake(toNodeId = payload.nodeId)
        }
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
        if (sent) counters?.onChatSent()
        return message.copy(state = if (sent) DeliveryState.SENT else DeliveryState.PENDING)
    }

    /**
     * Retries this node's own not-yet-delivered messages (docs/workflow.md §9). Call when a
     * new peer becomes reachable — e.g. the destination or a relay just came back into range.
     * Messages that exceed [maxRetries] are marked [DeliveryState.FAILED] and no longer retried.
     */
    suspend fun retryPendingMessages() {
        messages.pending().forEach { message ->
            if (message.retryCount >= maxRetries) {
                messages.updateState(message.packetId, DeliveryState.FAILED)
                return@forEach
            }
            val key = sessionKeyFor(message.receiverId) ?: return@forEach
            val sealed = MessageCipher.sealPayload(
                key,
                ChatPayload(message.text, message.replyToId),
            )
            val packet = Packet(
                packetId = message.packetId,
                type = PacketType.CHAT,
                senderId = message.senderId,
                receiverId = message.receiverId,
                timestamp = message.timestamp,
                ttl = Packet.DEFAULT_TTL,
                priority = Priority.NORMAL,
                payload = sealed,
            )
            val sent = dispatch(packet)
            if (sent) {
                messages.updateState(message.packetId, DeliveryState.SENT)
            } else {
                messages.incrementRetryCount(message.packetId)
            }
        }
    }

    /**
     * Retries packets this node is relaying for others but had no route for yet
     * (docs/routing.md §6 store-and-forward). Call when a new peer becomes reachable.
     *
     * Deliberately does NOT call [RoutingEngine.route] again: that would re-run the dedup
     * check against a `packetId` this node already marked seen the first time it evaluated
     * the packet (when it was queued), so every retry would be incorrectly dropped as a
     * duplicate. Once a packet is queued, retrying is purely a transport-reachability
     * question — the routing *decision* (relay vs. deliver, TTL budget) was already made.
     */
    suspend fun retryRelayQueue() {
        val queue = relayQueue ?: return
        queue.all().forEach { packet ->
            if (packet.isExpired) {
                queue.remove(packet.packetId)
                return@forEach
            }
            val direct = directPeerIds()
            val relay = relayPeerIds()
            val forwarded = when {
                packet.isBroadcast -> relay.filter { it != packet.senderId }
                    .map { sendTo(packet, it) }.any { it }
                packet.receiverId in direct -> sendTo(packet, packet.receiverId)
                else -> relay.filter { it != packet.senderId }
                    .map { sendTo(packet, it) }.any { it }
            }
            if (forwarded) queue.remove(packet.packetId)
        }
    }

    /**
     * A peer became reachable at the transport/radio level (freshly discovered or reconnected
     * after an outage) — drain both retry queues in case work was waiting for this contact.
     * Does NOT establish a session by itself; see the note in [start].
     */
    private suspend fun onPeerReachable(@Suppress("UNUSED_PARAMETER") nodeId: String) {
        retryPendingMessages()
        retryRelayQueue()
    }

    /**
     * A transport-level link to a peer dropped or failed to (re)connect. An ACTIVE session
     * whose underlying link just went away is no longer really active — mark it INTERRUPTED
     * so the UI reflects "offline" instead of a stale ACTIVE chip, without discarding the
     * session (public keys / packet counters), since a link drop is expected to be transient
     * (e.g. brief BLE range loss) rather than the peer being gone for good.
     */
    private suspend fun onLinkStateChanged(event: TransportEvent.LinkStateChanged) {
        if (event.state != LinkState.DISCONNECTED && event.state != LinkState.INTERRUPTED) return
        val peer = peers.observePeers().first().firstOrNull { it.nodeId == event.nodeId } ?: return
        if (peer.sessionState == SessionState.CLOSED) return
        peers.upsertPeer(peer.copy(sessionState = SessionState.INTERRUPTED))
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
            is RoutingDecision.ForwardDirect -> {
                // The routing engine trusts the session table, but the peer may be ACTIVE yet
                // transiently unreachable at the transport level (e.g. briefly offline). Fall
                // back to store-and-forward rather than silently dropping (docs/routing.md §6).
                if (!sendTo(decision.packet, decision.nextHop)) {
                    relayQueue?.enqueue(decision.packet)
                }
            }
            is RoutingDecision.Relay -> {
                val anySent = decision.neighbors.map { sendTo(decision.packet, it) }.any { it }
                if (anySent) counters?.onRelayed() else relayQueue?.enqueue(decision.packet)
            }
            is RoutingDecision.Queue -> relayQueue?.enqueue(decision.packet)
        }
    }
    private suspend fun deliverLocally(packet: Packet) {
        when (packet.type) {
            PacketType.CHAT -> persistIncomingChat(packet)
            PacketType.ACK -> handleAck(packet)
            PacketType.HANDSHAKE -> handleHandshake(packet)
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
        counters?.onChatReceived()
        sendAck(packet)
    }

    /** Sends a signed-by-encryption ACK back to [packet]'s sender (docs/protocol.md §16). */
    private suspend fun sendAck(packet: Packet) {
        val self = identity.nodeId()
        val key = sessionKeyFor(packet.senderId) ?: return
        val sealed = MessageCipher.sealPayload(
            key,
            AckPayload(
                acknowledgedPacketId = packet.packetId,
                status = AckPayload.Status.DELIVERED,
                nodeId = self,
            ),
        )
        val ack = Packet(
            packetId = UUID.randomUUID().toString(),
            type = PacketType.ACK,
            senderId = self,
            receiverId = packet.senderId,
            timestamp = clock(),
            ttl = Packet.DEFAULT_TTL,
            priority = Priority.NORMAL,
            payload = sealed,
        )
        dispatch(ack)
        counters?.onAckSent()
    }

    /** An ACK arrived for one of our own sent messages: mark it DELIVERED. */
    private suspend fun handleAck(packet: Packet) {
        val key = sessionKeyFor(packet.senderId) ?: return
        val payload = runCatching { MessageCipher.openPayload(key, packet.payload) }
            .getOrNull() as? AckPayload ?: return
        if (payload.status == AckPayload.Status.DELIVERED) {
            messages.updateState(payload.acknowledgedPacketId, DeliveryState.DELIVERED)
            counters?.onAckReceived()
        }
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
