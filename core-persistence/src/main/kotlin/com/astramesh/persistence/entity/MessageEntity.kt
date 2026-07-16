package com.astramesh.persistence.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Persisted chat message (docs/protocol.md §20 table `messages`). */
@Entity(
    tableName = "messages",
    indices = [Index("packetId", unique = true), Index("peerId")],
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val packetId: String,
    val senderId: String,
    val receiverId: String,
    /** The other party in the conversation (the peer), for grouping threads. */
    val peerId: String,
    val text: String,
    val timestamp: Long,
    val state: String,
    val outgoing: Boolean,
    val hopCount: Int,
    val replyToId: String? = null,
)
