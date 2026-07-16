package com.astramesh.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.astramesh.persistence.entity.FileTransferEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FileTransferDao {

    @Query("SELECT * FROM file_transfers")
    fun observeTransfers(): Flow<List<FileTransferEntity>>

    @Query("SELECT * FROM file_transfers WHERE fileId = :fileId LIMIT 1")
    suspend fun get(fileId: String): FileTransferEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(transfer: FileTransferEntity)
}
