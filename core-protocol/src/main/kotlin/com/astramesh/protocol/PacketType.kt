package com.astramesh.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Packet categories carried by the AstraMesh envelope (docs/protocol.md §9).
 */
@Serializable
enum class PacketType {
    /** Announces that a node is nearby. */
    @SerialName("presence")
    PRESENCE,

    /** Establishes a secure session (key exchange). */
    @SerialName("handshake")
    HANDSHAKE,

    /** One-to-one / one-to-few user chat message. */
    @SerialName("chat")
    CHAT,

    /** Emergency or community-wide announcement. */
    @SerialName("broadcast")
    BROADCAST,

    /** A fragment of a file transfer. */
    @SerialName("file_chunk")
    FILE_CHUNK,

    /** Confirms receipt or delivery of another packet. */
    @SerialName("ack")
    ACK,

    /** Shares knowledge of known/queued packets between neighbors. */
    @SerialName("routing_summary")
    ROUTING_SUMMARY,

    /** Diagnostics: battery, queue length, storage, transport status. */
    @SerialName("health")
    HEALTH,
}
