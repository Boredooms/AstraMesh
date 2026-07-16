package com.astramesh.persistence.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Persisted file transfer metadata (docs/protocol.md §20 table `file_transfers`). */
@Entity(tableName = "file_transfers")
data class FileTransferEntity(
    @PrimaryKey val fileId: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val totalChunks: Int,
    val fileHash: String,
    val senderId: String,
    val receiverId: String,
    val outgoing: Boolean,
    /** Received chunk indices as a comma-separated list. */
    val receivedChunks: String,
    val state: String,
)
