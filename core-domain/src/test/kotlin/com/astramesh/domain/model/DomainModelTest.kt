package com.astramesh.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainModelTest {

    @Test
    fun deliveryState_terminalStates() {
        assertTrue(DeliveryState.DELIVERED.isTerminal)
        assertTrue(DeliveryState.FAILED.isTerminal)
        assertTrue(DeliveryState.EXPIRED.isTerminal)
        assertFalse(DeliveryState.PENDING.isTerminal)
        assertFalse(DeliveryState.SENT.isTerminal)
    }

    @Test
    fun node_supportsRelay_requiresCapabilityAndFlag() {
        val base = Node(
            nodeId = "n1",
            deviceName = "d",
            platformType = PlatformType.ANDROID,
            publicKey = "pk",
            keyFingerprint = "fp",
            capabilities = setOf(Capability.CHAT, Capability.RELAY),
            lastSeen = 0,
            relayCapable = true,
        )
        assertTrue(base.supportsRelay)
        assertFalse(base.copy(relayCapable = false).supportsRelay)
        assertFalse(base.copy(capabilities = setOf(Capability.CHAT)).supportsRelay)
    }

    @Test
    fun fileTransfer_progressAndMissingChunks() {
        val ft = FileTransfer(
            fileId = "f",
            fileName = "a.bin",
            mimeType = "application/octet-stream",
            sizeBytes = 100,
            totalChunks = 4,
            fileHash = "h",
            senderId = "a",
            receiverId = "b",
            outgoing = false,
            receivedChunks = setOf(0, 2),
        )
        assertEquals(0.5f, ft.progress, 0.0001f)
        assertFalse(ft.isComplete)
        assertEquals(listOf(1, 3), ft.missingChunks())

        val done = ft.copy(receivedChunks = setOf(0, 1, 2, 3))
        assertTrue(done.isComplete)
        assertTrue(done.missingChunks().isEmpty())
    }

    @Test
    fun broadcast_expiry() {
        val b = Broadcast(
            id = "b", packetId = "p", senderId = "a", text = "help",
            severity = Broadcast.Severity.CRITICAL, timestamp = 0, expiresAt = 1000,
            outgoing = true,
        )
        assertFalse(b.isExpired(500))
        assertTrue(b.isExpired(1000))
        assertTrue(b.isExpired(2000))
    }

    @Test
    fun peer_connectedOnlyWhenActive() {
        val node = Node(
            "n", "d", PlatformType.ANDROID, "pk", "fp",
            setOf(Capability.CHAT), 0,
        )
        val active = Peer(node, SessionState.ACTIVE, -60, 0)
        val discovered = Peer(node, SessionState.DISCOVERED, -60, 0)
        assertTrue(active.isConnected)
        assertFalse(discovered.isConnected)
    }
}
