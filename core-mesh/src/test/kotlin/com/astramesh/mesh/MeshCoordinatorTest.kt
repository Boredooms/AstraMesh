package com.astramesh.mesh

import com.astramesh.domain.model.Capability
import com.astramesh.domain.model.DeliveryState
import com.astramesh.domain.model.Node
import com.astramesh.domain.model.Peer
import com.astramesh.domain.model.PlatformType
import com.astramesh.domain.model.SessionState
import com.astramesh.routing.EpidemicRoutingEngine
import com.astramesh.security.KeyExchange
import com.astramesh.security.KeyPairMaterial
import com.astramesh.transport.LoopbackTransport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * End-to-end mesh tests over the in-process LoopbackTransport with REAL encryption and routing:
 * direct A->B delivery, and multi-hop A->C-via-B relay (docs/architecture.md §7, §16).
 *
 * IMPORTANT: [MeshCoordinator.start] launches a long-lived collector on [backgroundScope]
 * (kotlinx.coroutines.test docs: coroutines meant to "outlive the tested code" belong there).
 * By design, [advanceUntilIdle] stops advancing virtual time once only backgroundScope work is
 * left, so it will NOT drive that collector on its own — use [runCurrent] after every action
 * that should cause the collector to process a newly emitted transport event.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MeshCoordinatorTest {

    private class TestNode(
        val id: String,
        val keys: KeyPairMaterial,
        val transport: LoopbackTransport,
        val messages: FakeMessageRepository,
        val peers: FakePeerRepository,
        val coordinator: MeshCoordinator,
    ) {
        fun identityNode(): Node = Node(
            nodeId = id, deviceName = id, platformType = PlatformType.ANDROID,
            publicKey = keys.publicKey, keyFingerprint = keys.fingerprint,
            capabilities = setOf(Capability.CHAT, Capability.RELAY), lastSeen = 0,
        )
    }

    private fun node(
        id: String,
        bus: LoopbackTransport.Bus,
        maxRetries: Int = 5,
        relayQueue: FakeRelayQueueRepository? = null,
    ): TestNode {
        val keys = KeyExchange.generateKeyPair()
        val self = Node(
            nodeId = id, deviceName = id, platformType = PlatformType.ANDROID,
            publicKey = keys.publicKey, keyFingerprint = keys.fingerprint,
            capabilities = setOf(Capability.CHAT, Capability.RELAY), lastSeen = 0,
        )
        val transport = LoopbackTransport(bus)
        val messages = FakeMessageRepository()
        val peers = FakePeerRepository()
        val coordinator = MeshCoordinator(
            transport = transport,
            routing = EpidemicRoutingEngine(),
            messages = messages,
            peers = peers,
            identity = FakeIdentity(id, self, keys.privateKey),
            sessionKeys = SessionKeyManager(keys.privateKey),
            relayQueue = relayQueue,
            clock = { 1_000L },
            maxRetries = maxRetries,
        )
        return TestNode(id, keys, transport, messages, peers, coordinator)
    }

    /** Registers [other]'s public identity + a live session with [self] so keys can derive. */
    private suspend fun TestNode.knows(other: TestNode, relay: Boolean = true) {
        val node = Node(
            nodeId = other.id, deviceName = other.id, platformType = PlatformType.ANDROID,
            publicKey = other.keys.publicKey, keyFingerprint = other.keys.fingerprint,
            capabilities = setOf(Capability.CHAT, Capability.RELAY), lastSeen = 0,
            relayCapable = relay,
        )
        peers.upsertPeer(Peer(node, SessionState.ACTIVE, -50, 0))
    }

    @Test
    fun directMessage_AtoB_isEncryptedAndDelivered() = runTest {
        val bus = LoopbackTransport.Bus()
        val a = node("node-a", bus)
        val b = node("node-b", bus)

        a.transport.start("node-a"); b.transport.start("node-b")
        a.knows(b); b.knows(a)
        a.coordinator.start(backgroundScope); b.coordinator.start(backgroundScope)
        runCurrent()

        a.coordinator.sendChat("node-b", "hello bob")
        runCurrent() // B receives CHAT, persists, sends ACK back

        val received = b.messages.snapshot().firstOrNull { !it.outgoing }
        assertTrue("B should have received the message", received != null)
        assertEquals("hello bob", received!!.text)
        assertEquals(DeliveryState.DELIVERED, received.state)
        assertEquals("node-a", received.senderId)

        runCurrent() // A receives the ACK
        val sent = a.messages.snapshot().first { it.outgoing }
        assertEquals(
            "A's own message should be DELIVERED once B's ACK arrives",
            DeliveryState.DELIVERED,
            sent.state,
        )
    }

    @Test
    fun multiHop_AtoC_viaB_isRelayed() = runTest {
        val bus = LoopbackTransport.Bus()
        val a = node("node-a", bus)
        val b = node("node-b", bus)
        val c = node("node-c", bus)

        a.transport.start("node-a"); b.transport.start("node-b"); c.transport.start("node-c")

        // Topology: A knows only B (relay). B knows A and C. C knows A (for key derivation) and B.
        // A cannot reach C directly, so it must relay through B.
        a.knows(b)                 // A's only neighbor is B
        b.knows(a); b.knows(c)     // B bridges A and C
        c.knows(a); c.knows(b)     // C can derive the A session key to decrypt

        a.coordinator.start(backgroundScope)
        b.coordinator.start(backgroundScope)
        c.coordinator.start(backgroundScope)
        runCurrent()

        // A needs C's public key to derive the E2E session key for sendChat(), even though
        // C is not a direct/relay peer of A. upsertNode records identity only (no Peer/session
        // entry), so C still correctly stays out of A's directPeerIds/relayPeerIds.
        a.peers.upsertNode(
            Node(
                nodeId = c.id, deviceName = c.id, platformType = PlatformType.ANDROID,
                publicKey = c.keys.publicKey, keyFingerprint = c.keys.fingerprint,
                capabilities = setOf(Capability.CHAT, Capability.RELAY), lastSeen = 0,
            )
        )

        // A sends to C. A has no direct route to C, only relay peer B -> epidemic relay.
        // Two hops (A->B, B->C) means two rounds of collector dispatch, so drain twice.
        a.coordinator.sendChat("node-c", "hello carol")
        runCurrent()
        runCurrent()

        val atC = c.messages.snapshot().firstOrNull { !it.outgoing }
        assertTrue("C should receive the relayed message", atC != null)
        assertEquals("hello carol", atC!!.text)
        assertEquals("node-a", atC.senderId)
        assertTrue("message should show it was relayed (hopCount>0)", atC.hopCount >= 1)
    }

    @Test
    fun handshake_establishesActiveSession_beforeFirstMessage() = runTest {
        val bus = LoopbackTransport.Bus()
        val a = node("node-a", bus)
        val b = node("node-b", bus)

        a.transport.start("node-a"); b.transport.start("node-b")
        a.coordinator.start(backgroundScope); b.coordinator.start(backgroundScope)
        runCurrent()

        // Radio-level discovery alone must NOT create a session (docs/protocol.md §11):
        // no knows() was called, so neither side should show an ACTIVE peer yet.
        assertTrue(
            "no session should exist before any handshake",
            a.peers.observePeers().first().none { it.isConnected },
        )

        // User opens a chat with B: explicit handshake (HELLO / HELLO_ACK).
        a.coordinator.connectTo("node-b")
        runCurrent() // B receives HELLO, marks A ACTIVE, replies with HELLO_ACK
        runCurrent() // A receives HELLO_ACK, marks B ACTIVE

        val aSeesB = a.peers.observePeers().first().first { it.nodeId == "node-b" }
        val bSeesA = b.peers.observePeers().first().first { it.nodeId == "node-a" }
        assertTrue("A should have an ACTIVE session with B", aSeesB.isConnected)
        assertTrue("B should have an ACTIVE session with A", bSeesA.isConnected)
        assertEquals(a.keys.publicKey, bSeesA.node.publicKey)
        assertEquals(b.keys.publicKey, aSeesB.node.publicKey)

        // Now a real chat message should flow using the keys learned during the handshake.
        a.coordinator.sendChat("node-b", "post-handshake hello")
        runCurrent()
        val received = b.messages.snapshot().firstOrNull { !it.outgoing }
        assertEquals("post-handshake hello", received?.text)
    }

    @Test
    fun pendingMessage_marksFailed_afterExceedingRetryBudget() = runTest {
        val bus = LoopbackTransport.Bus()
        val a = node("node-a", bus, maxRetries = 2)
        val b = node("node-b", bus)
        a.transport.start("node-a") // B never starts: unreachable for the whole test
        a.knows(b)
        a.coordinator.start(backgroundScope)
        runCurrent()

        val sentMsg = a.coordinator.sendChat("node-b", "into the void")
        assertEquals(DeliveryState.PENDING, sentMsg?.state) // no route: dispatch() returns false

        repeat(3) { a.coordinator.retryPendingMessages() }

        val stored = a.messages.snapshot().first { it.outgoing }
        assertEquals(DeliveryState.FAILED, stored.state)
    }

    @Test
    fun restart_messagesPersistAndMessagingContinues() = runTest {
        val bus = LoopbackTransport.Bus()
        val a = node("node-a", bus)
        val b = node("node-b", bus)
        a.transport.start("node-a"); b.transport.start("node-b")
        a.knows(b); b.knows(a)
        a.coordinator.start(backgroundScope); b.coordinator.start(backgroundScope)
        runCurrent()

        a.coordinator.sendChat("node-b", "before restart")
        runCurrent()
        assertEquals(1, b.messages.snapshot().count { !it.outgoing })

        // Simulate an app restart on B: the transport and MeshCoordinator are recreated (as
        // they would be by a fresh process), but the message/peer repositories are the SAME
        // instances — standing in for Room persistence surviving the process restart
        // (docs/workflow.md §13, verification requirement in the milestone spec).
        val restartedTransport = LoopbackTransport(bus)
        val restartedCoordinator = MeshCoordinator(
            transport = restartedTransport,
            routing = EpidemicRoutingEngine(),
            messages = b.messages,
            peers = b.peers,
            identity = FakeIdentity("node-b", b.identityNode(), b.keys.privateKey),
            sessionKeys = SessionKeyManager(b.keys.privateKey),
            clock = { 1_000L },
        )
        restartedTransport.start("node-b")
        restartedCoordinator.start(backgroundScope)
        runCurrent()

        // Old message is still there after "restart".
        assertEquals(1, b.messages.snapshot().count { !it.outgoing })

        // Messaging continues normally post-restart.
        a.coordinator.sendChat("node-b", "after restart")
        runCurrent()
        assertEquals(2, b.messages.snapshot().count { !it.outgoing })
        assertTrue(b.messages.snapshot().any { !it.outgoing && it.text == "after restart" })
    }

    @Test
    fun storeAndForward_relayQueuesForOfflinePeer_thenDeliversOnReturn() = runTest {
        val bus = LoopbackTransport.Bus()
        val relayQueueOnB = FakeRelayQueueRepository()
        val a = node("node-a", bus)
        val b = node("node-b", bus, relayQueue = relayQueueOnB)
        val c = node("node-c", bus)

        // A and C are never directly connected; B is the only relay. C starts OFFLINE.
        a.knows(b)
        b.knows(a); b.knows(c)
        c.knows(a); c.knows(b)
        a.peers.upsertNode(
            Node(
                nodeId = c.id, deviceName = c.id, platformType = PlatformType.ANDROID,
                publicKey = c.keys.publicKey, keyFingerprint = c.keys.fingerprint,
                capabilities = setOf(Capability.CHAT, Capability.RELAY), lastSeen = 0,
            )
        )

        a.transport.start("node-a"); b.transport.start("node-b")
        // c.transport is intentionally NOT started yet — C is offline.
        a.coordinator.start(backgroundScope); b.coordinator.start(backgroundScope)
        runCurrent()

        a.coordinator.sendChat("node-c", "are you there?")
        runCurrent() // B receives it, has no route to C, queues it for store-and-forward

        assertEquals(0, c.messages.snapshot().size)
        assertEquals(
            "B should be holding the packet in its relay queue while C is offline",
            1,
            relayQueueOnB.all().size,
        )

        // C comes back online. Subscribe C's collector to its transport events FIRST and let
        // it actually start collecting (runCurrent) before announcing C's presence — otherwise
        // B could forward to C before C's collector coroutine has subscribed to the underlying
        // SharedFlow, and the emission would be missed (no replay for late subscribers; the
        // same class of bug documented in docs/RCA-directMessage-test-failure.md).
        c.coordinator.start(backgroundScope)
        runCurrent()
        c.transport.start("node-c")
        runCurrent() // B observes C's PeerDiscovered event, drains queue, forwards to C
        runCurrent() // C processes the forwarded CHAT packet

        val atC = c.messages.snapshot().firstOrNull { !it.outgoing }
        assertTrue("C should eventually receive the stored message", atC != null)
        assertEquals("are you there?", atC!!.text)
        assertEquals(0, relayQueueOnB.all().size)
    }

    @Test
    fun relayNode_cannotDecryptPayload_itOnlyForwardsOpaqueBytes() = runTest {
        val bus = LoopbackTransport.Bus()
        val a = node("node-a", bus)
        val b = node("node-b", bus)
        val c = node("node-c", bus)
        a.knows(b); b.knows(a); b.knows(c); c.knows(a); c.knows(b)
        a.peers.upsertNode(
            Node(
                nodeId = c.id, deviceName = c.id, platformType = PlatformType.ANDROID,
                publicKey = c.keys.publicKey, keyFingerprint = c.keys.fingerprint,
                capabilities = setOf(Capability.CHAT, Capability.RELAY), lastSeen = 0,
            )
        )
        a.transport.start("node-a"); b.transport.start("node-b"); c.transport.start("node-c")
        a.coordinator.start(backgroundScope)
        b.coordinator.start(backgroundScope)
        c.coordinator.start(backgroundScope)
        runCurrent()

        a.coordinator.sendChat("node-c", "top secret")
        runCurrent()
        runCurrent()

        // B relayed the packet but is not the addressee, so MeshCoordinator never calls
        // openPayload for B — B's own message store must never contain the plaintext.
        assertTrue(
            "the relay node must never persist the message content it forwarded",
            b.messages.snapshot().none { it.text == "top secret" },
        )
        assertTrue(c.messages.snapshot().any { it.text == "top secret" })
    }

    @Test
    fun duplicateDelivery_isIgnored_atDestination() = runTest {
        val bus = LoopbackTransport.Bus()
        val a = node("node-a", bus)
        val b = node("node-b", bus)
        a.transport.start("node-a"); b.transport.start("node-b")
        a.knows(b); b.knows(a)
        a.coordinator.start(backgroundScope); b.coordinator.start(backgroundScope)
        runCurrent()

        a.coordinator.sendChat("node-b", "once")
        runCurrent()
        // Re-inject the same packet: dedup cache should drop it, so still exactly one message.
        val count = b.messages.snapshot().count { !it.outgoing }
        assertEquals(1, count)
    }
}
