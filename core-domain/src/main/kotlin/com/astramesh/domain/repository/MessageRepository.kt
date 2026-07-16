package com.astramesh.domain.repository

import com.astramesh.domain.model.Message
import com.astramesh.domain.model.DeliveryState
import kotlinx.coroutines.flow.Flow

/**
 * Persistence + retrieval of chat messages. Implemented by the persistence layer
 * (docs/architecture.md §13). "Persist first, then send" — see docs/workflow.md §13.
 */
interface MessageRepository {
    /** All messages in a conversation with [peerId], oldest first, as a reactive stream. */
    fun observeConversation(peerId: String): Flow<List<Message>>

    /** Distinct peers that have a conversation, most-recent activity first. */
    fun observeConversations(): Flow<List<Message>>

    suspend fun getByPacketId(packetId: String): Message?

    /** Insert or update a message. */
    suspend fun upsert(message: Message)

    suspend fun updateState(packetId: String, state: DeliveryState)

    /** Messages awaiting delivery, for the store-and-forward retry loop. */
    suspend fun pending(): List<Message>

    /** Reactive count of pending/sent/relayed (not yet terminal) messages, for diagnostics. */
    fun observePendingCount(): Flow<Int>

    /** Bumps the store-and-forward retry counter for a message that failed to send again. */
    suspend fun incrementRetryCount(packetId: String)
}
