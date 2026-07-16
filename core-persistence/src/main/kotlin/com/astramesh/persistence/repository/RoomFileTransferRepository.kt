package com.astramesh.persistence.repository

import com.astramesh.domain.model.FileTransfer
import com.astramesh.domain.repository.FileTransferRepository
import com.astramesh.persistence.dao.FileTransferDao
import com.astramesh.persistence.repository.Mappers.toEntity
import com.astramesh.persistence.repository.Mappers.toTransfer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomFileTransferRepository @Inject constructor(
    private val dao: FileTransferDao,
) : FileTransferRepository {

    override fun observeTransfers(): Flow<List<FileTransfer>> =
        dao.observeTransfers().map { list -> list.map { it.toTransfer() } }

    override suspend fun get(fileId: String): FileTransfer? = dao.get(fileId)?.toTransfer()

    override suspend fun upsert(transfer: FileTransfer) = dao.upsert(transfer.toEntity())

    override suspend fun markChunkReceived(fileId: String, chunkIndex: Int): FileTransfer? {
        val current = dao.get(fileId)?.toTransfer() ?: return null
        val updated = current.copy(receivedChunks = current.receivedChunks + chunkIndex)
        dao.upsert(updated.toEntity())
        return updated
    }
}
