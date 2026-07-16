package com.astramesh.domain.model

/**
 * A user-facing chat message (docs/workflow.md §7).
 *
 * This is the app-level view of a conversation entry. Its content travels the wire as an
 * encrypted [com.astramesh.protocol.payload.ChatPayload] inside a
 * [com.astramesh.protocol.Packet]; [packetId] links the two.
 */
data class Message(
    val id: String,
    val packetId: String,
    val senderId: String,
    val receiverId: String,
    val text: String,
    val timestamp: Long,
    val state: DeliveryState,
    val outgoing: Boolean,
    val hopCount: Int = 0,
    val replyToId: String? = null,
)
