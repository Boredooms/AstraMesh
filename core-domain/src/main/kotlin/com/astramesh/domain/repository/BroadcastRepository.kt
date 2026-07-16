package com.astramesh.domain.repository

import com.astramesh.domain.model.Broadcast
import kotlinx.coroutines.flow.Flow

/** Persistence + retrieval of emergency broadcasts (docs/workflow.md §11). */
interface BroadcastRepository {
    /** Broadcast feed, newest first. */
    fun observeBroadcasts(): Flow<List<Broadcast>>

    suspend fun getByPacketId(packetId: String): Broadcast?

    suspend fun upsert(broadcast: Broadcast)
}
