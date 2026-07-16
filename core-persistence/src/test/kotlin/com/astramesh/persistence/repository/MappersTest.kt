package com.astramesh.persistence.repository

import com.astramesh.domain.model.Broadcast
import com.astramesh.domain.model.Capability
import com.astramesh.domain.model.DeliveryState
import com.astramesh.domain.model.FileTransfer
import com.astramesh.domain.model.Message
import com.astramesh.domain.model.Node
import com.astramesh.domain.model.Peer
import com.astramesh.domain.model.PlatformType
import com.astramesh.domain.model.SessionState
import com.astramesh.persistence.repository.Mappers.toBroadcast
import com.astramesh.persistence.repository.Mappers.toEntity
import com.astramesh.persistence.repository.Mappers.toMessage
import com.astramesh.persistence.repository.Mappers.toNode
import com.astramesh.persistence.repository.Mappers.toPeerOrNull
import com.astramesh.persistence.repository.Mappers.toTransfer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure-JVM tests for entity <-> domain mapping (no Android instrumentation needed). */
class MappersTest {

    @Test
    fun node_roundTrips_throughEntity() {
        val node = Node(
            nodeId = "n1",
            deviceName = "Pixel",
            platformType = PlatformType.ANDROID,
            publicKey = "pk",
            keyFingerprint = "fp",
            capabilities = setOf(Capability.CHAT, Capability.RELAY),
            lastSeen = 42,
            relayCapable = true,
        )
        assertEquals(node, node.toEntity().toNode())
    }

    @Test
    fun nodeEntity_withoutSession_isNotAPeer() {
        val node = Node("n", "d", PlatformType.ANDROID, "pk", "fp", setOf(Capability.CHAT), 0)
        assertNull(node.toEntity().toPeerOrNull())
    }

    @Test
    fun peer_roundTrips_throughEntity() {
        val node = Node("n", "d", PlatformType.ANDROID, "pk", "fp", setOf(Capability.CHAT), 5)
        val peer = Peer(node, SessionState.ACTIVE, signalStrength = -50, lastContact = 99)
        val restored = peer.toEntity().toPeerOrNull()
        assertEquals(peer, restored)
    }

    @Test
    fun message_roundTrips_andComputesPeerId() {
        val outgoing = Message(
            id = "m1", packetId = "p1", senderId = "me", receiverId = "bob",
            text = "hi", timestamp = 1, state = DeliveryState.SENT, outgoing = true,
        )
        val entity = outgoing.toEntity()
        assertEquals("bob", entity.peerId) // outgoing -> peer is receiver
        assertEquals(outgoing, entity.toMessage())

        val incoming = outgoing.copy(outgoing = false)
        assertEquals("me", incoming.toEntity().peerId) // incoming -> peer is sender
    }

    @Test
    fun broadcast_roundTrips() {
        val b = Broadcast(
            id = "b", packetId = "p", senderId = "a", text = "flee",
            severity = Broadcast.Severity.CRITICAL, timestamp = 3, expiresAt = 10,
            outgoing = false, hopCount = 2,
        )
        assertEquals(b, b.toEntity().toBroadcast())
    }

    @Test
    fun fileTransfer_roundTrips_withChunkSet() {
        val ft = FileTransfer(
            fileId = "f", fileName = "a.pdf", mimeType = "application/pdf",
            sizeBytes = 100, totalChunks = 4, fileHash = "h",
            senderId = "a", receiverId = "b", outgoing = true,
            receivedChunks = setOf(2, 0, 1), state = DeliveryState.PENDING,
        )
        val restored = ft.toEntity().toTransfer()
        assertEquals(setOf(0, 1, 2), restored.receivedChunks)
        assertEquals(ft.copy(receivedChunks = setOf(0, 1, 2)), restored)
    }

    @Test
    fun fileTransfer_emptyChunkSet_roundTrips() {
        val ft = FileTransfer(
            fileId = "f", fileName = "a", mimeType = "text/plain",
            sizeBytes = 0, totalChunks = 1, fileHash = "h",
            senderId = "a", receiverId = "b", outgoing = false,
        )
        assertTrue(ft.toEntity().toTransfer().receivedChunks.isEmpty())
    }
}
