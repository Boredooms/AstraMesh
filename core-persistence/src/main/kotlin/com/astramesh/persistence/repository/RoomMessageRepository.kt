package com.astramesh.persistence.repository

import com.astramesh.domain.model.DeliveryState
import com.astramesh.domain.model.Message
import com.astramesh.domain.repository.MessageRepository
import com.astramesh.persistence.dao.MessageDao
import com.astramesh.persistence.repository.Mappers.toEntity
import com.astramesh.persistence.repository.Mappers.toMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomMessageRepository @Inject constructor(
    private val dao: MessageDao,
) : MessageRepository {

    override fun observeConversation(peerId: String): Flow<List<Message>> =
        dao.observeConversation(peerId).map { list -> list.map { it.toMessage() } }

    override fun observeConversations(): Flow<List<Message>> =
        dao.observeConversations().map { list -> list.map { it.toMessage() } }

    override suspend fun getByPacketId(packetId: String): Message? =
        dao.getByPacketId(packetId)?.toMessage()

    override suspend fun upsert(message: Message) = dao.upsert(message.toEntity())

    override suspend fun updateState(packetId: String, state: DeliveryState) =
        dao.updateState(packetId, state.name)

    override suspend fun pending(): List<Message> = dao.pending().map { it.toMessage() }

    override fun observePendingCount(): Flow<Int> = dao.observePendingCount()

    override suspend fun incrementRetryCount(packetId: String) = dao.incrementRetryCount(packetId)
}
