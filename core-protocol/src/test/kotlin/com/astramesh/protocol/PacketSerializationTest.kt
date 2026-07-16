package com.astramesh.protocol

import com.astramesh.protocol.payload.AckPayload
import com.astramesh.protocol.payload.BroadcastPayload
import com.astramesh.protocol.payload.ChatPayload
import com.astramesh.protocol.payload.FileChunkPayload
import com.astramesh.protocol.payload.HandshakePayload
import com.astramesh.protocol.payload.PacketPayload
import com.astramesh.protocol.payload.PresencePayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PacketSerializationTest {

    private fun sampleChatPacket() = Packet(
        packetId = "pkt-1",
        type = PacketType.CHAT,
        senderId = "node-a",
        receiverId = "node-b",
        timestamp = 1_700_000_000_000,
        ttl = Packet.DEFAULT_TTL,
        payload = "encrypted-bytes",
    )

    @Test
    fun packet_roundTrips_throughJson() {
        val original = sampleChatPacket()
        val text = ProtocolJson.encodePacket(original)
        val decoded = ProtocolJson.decodePacket(text)
        assertEquals(original, decoded)
    }

    @Test
    fun packet_defaults_areStable() {
        val p = sampleChatPacket()
        assertEquals(ProtocolVersion.CURRENT, p.protocolVersion)
        assertEquals(ProtocolVersion.PACKET_SCHEMA, p.packetVersion)
        assertEquals(0, p.hopCount)
        assertEquals(Priority.NORMAL, p.priority)
        assertNull(p.signature)
    }

    @Test
    fun unknownKeys_areIgnored_forForwardCompatibility() {
        val original = sampleChatPacket()
        val text = ProtocolJson.encodePacket(original)
        // A future protocol version adds a field this build doesn't know about.
        val withExtra = text.dropLast(1) + ""","futureField":"surprise"}"""
        val decoded = ProtocolJson.decodePacket(withExtra)
        assertEquals(original, decoded)
    }

    @Test
    fun broadcast_packet_isFlaggedByReceiver() {
        val p = sampleChatPacket().copy(
            type = PacketType.BROADCAST,
            receiverId = Packet.BROADCAST_RECEIVER,
            priority = Priority.EMERGENCY,
        )
        assertTrue(p.isBroadcast)
    }

    @Test
    fun relayed_advancesHop_andDecrementsTtl() {
        val p = sampleChatPacket().copy(ttl = 3, hopCount = 1)
        val r = p.relayed()
        assertEquals(2, r.ttl)
        assertEquals(2, r.hopCount)
    }

    @Test
    fun expired_whenTtlZero() {
        val p = sampleChatPacket().copy(ttl = 0)
        assertTrue(p.isExpired)
        assertTrue(!p.canRelay)
    }

    // ---- Typed payloads ----

    private fun assertPayloadRoundTrips(payload: PacketPayload) {
        val text = ProtocolJson.encodePayload(payload)
        val decoded = ProtocolJson.decodePayload(text)
        assertEquals(payload, decoded)
    }

    @Test
    fun allPayloadTypes_roundTrip() {
        assertPayloadRoundTrips(
            PresencePayload(
                nodeId = "node-a",
                deviceName = "Pixel",
                platformType = "android",
                protocolVersion = ProtocolVersion.CURRENT,
                capabilities = listOf("chat", "relay"),
            )
        )
        assertPayloadRoundTrips(
            HandshakePayload(
                nodeId = "node-a",
                publicKey = "pk",
                nonce = "n",
                protocolVersion = ProtocolVersion.CURRENT,
                capabilities = listOf("chat"),
            )
        )
        assertPayloadRoundTrips(ChatPayload(text = "hello", replyToPacketId = "pkt-0"))
        assertPayloadRoundTrips(
            BroadcastPayload(text = "flood", severity = BroadcastPayload.Severity.CRITICAL)
        )
        assertPayloadRoundTrips(
            FileChunkPayload(
                fileId = "f1", fileName = "a.txt", mimeType = "text/plain",
                chunkIndex = 2, totalChunks = 5, fileHash = "h", chunkHash = "ch", data = "AAAA",
            )
        )
        assertPayloadRoundTrips(
            AckPayload(
                acknowledgedPacketId = "pkt-1",
                status = AckPayload.Status.DELIVERED,
                nodeId = "node-b",
            )
        )
    }

    @Test
    fun sealedPayload_discriminator_distinguishesTypes() {
        val chat: PacketPayload = ChatPayload("hi")
        val ack: PacketPayload = AckPayload("p", AckPayload.Status.RECEIVED, "n")
        val chatText = ProtocolJson.encodePayload(chat)
        val ackText = ProtocolJson.encodePayload(ack)
        // Different concrete types must not decode to each other.
        assertTrue(ProtocolJson.decodePayload(chatText) is ChatPayload)
        assertTrue(ProtocolJson.decodePayload(ackText) is AckPayload)
    }
}
