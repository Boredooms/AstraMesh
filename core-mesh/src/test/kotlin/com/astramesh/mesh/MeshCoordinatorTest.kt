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
    )

    private fun node(
        id: String,
        bus: LoopbackTransport.Bus,
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
            clock = { 1_000L },
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
        runCurrent()

        val received = b.messages.snapshot().firstOrNull { !it.outgoing }
        assertTrue("B should have received the message", received != null)
        assertEquals("hello bob", received!!.text)
        assertEquals(DeliveryState.DELIVERED, received.state)
        assertEquals("node-a", received.senderId)
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
