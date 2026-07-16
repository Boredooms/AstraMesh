package com.astramesh.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.astramesh.persistence.entity.RelayQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RelayQueueDao {

    @Query("SELECT COUNT(*) FROM relay_queue")
    fun observeQueueSize(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RelayQueueEntity)

    @Query("SELECT * FROM relay_queue ORDER BY queuedAt ASC")
    suspend fun all(): List<RelayQueueEntity>

    @Query("DELETE FROM relay_queue WHERE packetId = :packetId")
    suspend fun remove(packetId: String)
}
