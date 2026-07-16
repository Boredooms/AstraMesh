package com.astramesh.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.astramesh.persistence.entity.NodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NodeDao {

    @Query("SELECT * FROM nodes WHERE sessionState IS NOT NULL ORDER BY lastContact DESC")
    fun observePeers(): Flow<List<NodeEntity>>

    @Query("SELECT * FROM nodes WHERE nodeId = :nodeId LIMIT 1")
    suspend fun getNode(nodeId: String): NodeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(node: NodeEntity)

    @Query("DELETE FROM nodes WHERE nodeId = :nodeId")
    suspend fun delete(nodeId: String)
}
