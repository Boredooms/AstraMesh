package com.astramesh.persistence

import androidx.room.Database
import androidx.room.RoomDatabase
import com.astramesh.persistence.dao.BroadcastDao
import com.astramesh.persistence.dao.FileTransferDao
import com.astramesh.persistence.dao.MessageDao
import com.astramesh.persistence.dao.NodeDao
import com.astramesh.persistence.entity.BroadcastEntity
import com.astramesh.persistence.entity.FileTransferEntity
import com.astramesh.persistence.entity.MessageEntity
import com.astramesh.persistence.entity.NodeEntity

/**
 * Local Room database — the source of truth for the mesh (docs/workflow.md §13).
 * "Persist first, then send."
 */
@Database(
    entities = [
        NodeEntity::class,
        MessageEntity::class,
        BroadcastEntity::class,
        FileTransferEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AstraMeshDatabase : RoomDatabase() {
    abstract fun nodeDao(): NodeDao
    abstract fun messageDao(): MessageDao
    abstract fun broadcastDao(): BroadcastDao
    abstract fun fileTransferDao(): FileTransferDao

    companion object {
        const val NAME = "astramesh.db"
    }
}
