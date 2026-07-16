package com.astramesh.domain.model

/**
 * Metadata for a chunked file transfer (docs/protocol.md §17, docs/workflow.md §10).
 *
 * The file is split into [totalChunks] fragments, each sent as an encrypted
 * [com.astramesh.protocol.payload.FileChunkPayload]. Receivers reassemble in any order and
 * verify [fileHash] before completing.
 */
data class FileTransfer(
    val fileId: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val totalChunks: Int,
    val fileHash: String,
    val senderId: String,
    val receiverId: String,
    val outgoing: Boolean,
    val receivedChunks: Set<Int> = emptySet(),
    val state: DeliveryState = DeliveryState.PENDING,
) {
    val isComplete: Boolean get() = receivedChunks.size == totalChunks

    val progress: Float
        get() = if (totalChunks == 0) 0f else receivedChunks.size.toFloat() / totalChunks

    /** Chunk indices not yet received; used to request retransmission. */
    fun missingChunks(): List<Int> =
        (0 until totalChunks).filter { it !in receivedChunks }
}
