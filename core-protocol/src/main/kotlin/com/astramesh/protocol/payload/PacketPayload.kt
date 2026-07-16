package com.astramesh.protocol.payload

import kotlinx.serialization.Serializable

/**
 * Typed packet payloads (docs/protocol.md §9). Each is the *decrypted* content that a
 * sender serializes and encrypts into [com.astramesh.protocol.Packet.payload]. Relay nodes
 * never deserialize these — only the destination does, after decryption.
 *
 * They are grouped under one sealed hierarchy so serialization is exhaustive and type-safe.
 */
@Serializable
sealed interface PacketPayload

/** Node presence announcement + capability summary. */
@Serializable
data class PresencePayload(
    val nodeId: String,
    val deviceName: String,
    val platformType: String,
    val protocolVersion: String,
    val capabilities: List<String>,
    val relaySupported: Boolean = true,
) : PacketPayload

/** Handshake / key-exchange material. */
@Serializable
data class HandshakePayload(
    val nodeId: String,
    val publicKey: String,
    val nonce: String,
    val protocolVersion: String,
    val capabilities: List<String>,
) : PacketPayload

/** One-to-one chat message content. */
@Serializable
data class ChatPayload(
    val text: String,
    val replyToPacketId: String? = null,
) : PacketPayload

/** Emergency / community broadcast content. */
@Serializable
data class BroadcastPayload(
    val text: String,
    val severity: Severity = Severity.INFO,
    val expiresAt: Long? = null,
) : PacketPayload {
    @Serializable
    enum class Severity { INFO, WARNING, CRITICAL }
}

/** A single fragment of a file transfer. */
@Serializable
data class FileChunkPayload(
    val fileId: String,
    val fileName: String,
    val mimeType: String,
    val chunkIndex: Int,
    val totalChunks: Int,
    val fileHash: String,
    val chunkHash: String,
    /** Base64-encoded chunk bytes. */
    val data: String,
) : PacketPayload

/** Acknowledgment of another packet. */
@Serializable
data class AckPayload(
    val acknowledgedPacketId: String,
    val status: Status,
    val nodeId: String,
) : PacketPayload {
    @Serializable
    enum class Status { RECEIVED, DELIVERED, FAILED }
}
