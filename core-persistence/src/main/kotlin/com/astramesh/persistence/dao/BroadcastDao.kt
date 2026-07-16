package com.astramesh.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.astramesh.persistence.entity.BroadcastEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BroadcastDao {

    @Query("SELECT * FROM broadcasts ORDER BY timestamp DESC")
    fun observeBroadcasts(): Flow<List<BroadcastEntity>>

    @Query("SELECT * FROM broadcasts WHERE packetId = :packetId LIMIT 1")
    suspend fun getByPacketId(packetId: String): BroadcastEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(broadcast: BroadcastEntity)
}
