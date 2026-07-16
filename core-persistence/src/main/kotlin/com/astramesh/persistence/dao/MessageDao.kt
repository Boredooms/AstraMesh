package com.astramesh.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.astramesh.persistence.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE peerId = :peerId ORDER BY timestamp ASC")
    fun observeConversation(peerId: String): Flow<List<MessageEntity>>

    /** One most-recent message per peer, newest first (conversation list). */
    @Query(
        """
        SELECT * FROM messages
        WHERE timestamp IN (SELECT MAX(timestamp) FROM messages GROUP BY peerId)
        ORDER BY timestamp DESC
        """
    )
    fun observeConversations(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE packetId = :packetId LIMIT 1")
    suspend fun getByPacketId(packetId: String): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: MessageEntity)

    @Query("UPDATE messages SET state = :state WHERE packetId = :packetId")
    suspend fun updateState(packetId: String, state: String)

    @Query("SELECT * FROM messages WHERE state IN ('PENDING', 'SENT', 'RELAYED') ORDER BY timestamp ASC")
    suspend fun pending(): List<MessageEntity>

    @Query("SELECT COUNT(*) FROM messages WHERE state IN ('PENDING', 'SENT', 'RELAYED')")
    fun observePendingCount(): Flow<Int>

    @Query("UPDATE messages SET retryCount = retryCount + 1 WHERE packetId = :packetId")
    suspend fun incrementRetryCount(packetId: String)
}
