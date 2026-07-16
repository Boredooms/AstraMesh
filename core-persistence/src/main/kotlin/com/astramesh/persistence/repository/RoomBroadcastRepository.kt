package com.astramesh.persistence.repository

import com.astramesh.domain.model.Broadcast
import com.astramesh.domain.repository.BroadcastRepository
import com.astramesh.persistence.dao.BroadcastDao
import com.astramesh.persistence.repository.Mappers.toBroadcast
import com.astramesh.persistence.repository.Mappers.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomBroadcastRepository @Inject constructor(
    private val dao: BroadcastDao,
) : BroadcastRepository {

    override fun observeBroadcasts(): Flow<List<Broadcast>> =
        dao.observeBroadcasts().map { list -> list.map { it.toBroadcast() } }

    override suspend fun getByPacketId(packetId: String): Broadcast? =
        dao.getByPacketId(packetId)?.toBroadcast()

    override suspend fun upsert(broadcast: Broadcast) = dao.upsert(broadcast.toEntity())
}
