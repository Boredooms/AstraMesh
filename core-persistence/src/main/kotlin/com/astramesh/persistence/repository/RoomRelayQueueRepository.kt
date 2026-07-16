package com.astramesh.persistence.repository

import com.astramesh.domain.repository.RelayQueueRepository
import com.astramesh.persistence.dao.RelayQueueDao
import com.astramesh.persistence.entity.RelayQueueEntity
import com.astramesh.protocol.Packet
import com.astramesh.protocol.ProtocolJson
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RoomRelayQueueRepository @Inject constructor(
    private val dao: RelayQueueDao,
) : RelayQueueRepository {

    override fun observeQueueSize(): Flow<Int> = dao.observeQueueSize()

    override suspend fun enqueue(packet: Packet) {
        dao.upsert(
            RelayQueueEntity(
                packetId = packet.packetId,
                payloadJson = ProtocolJson.encodePacket(packet),
                queuedAt = System.currentTimeMillis(),
            )
        )
    }

    override suspend fun all(): List<Packet> =
        dao.all().map { ProtocolJson.decodePacket(it.payloadJson) }

    override suspend fun remove(packetId: String) = dao.remove(packetId)
}
