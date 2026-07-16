package com.astramesh.domain.repository

import com.astramesh.domain.model.FileTransfer
import kotlinx.coroutines.flow.Flow

/** Persistence + retrieval of file transfers and their chunk state (docs/protocol.md §17). */
interface FileTransferRepository {
    fun observeTransfers(): Flow<List<FileTransfer>>

    suspend fun get(fileId: String): FileTransfer?

    suspend fun upsert(transfer: FileTransfer)

    /** Record that a chunk arrived; returns the updated transfer. */
    suspend fun markChunkReceived(fileId: String, chunkIndex: Int): FileTransfer?
}
