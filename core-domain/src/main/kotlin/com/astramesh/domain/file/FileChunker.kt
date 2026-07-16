package com.astramesh.domain.file

import com.astramesh.domain.model.FileTransfer
import com.astramesh.protocol.payload.FileChunkPayload
import java.security.MessageDigest
import java.util.Base64

/**
 * Splits files into chunks and reassembles them (docs/protocol.md §17, docs/workflow.md §10).
 *
 * Pure Kotlin: operates on byte arrays so it is unit-testable on the JVM and reused by both the
 * Android app and the desktop companion. Integrity is enforced with SHA-256 hashes over each
 * chunk and the whole file; reassembly verifies both.
 */
object FileChunker {

    /** Default chunk size kept conservative for BLE-friendly transport (docs/architecture.md §9). */
    const val DEFAULT_CHUNK_SIZE = 4 * 1024

    private val encoder: Base64.Encoder = Base64.getEncoder()
    private val decoder: Base64.Decoder = Base64.getDecoder()

    /**
     * Splits [bytes] into [FileChunkPayload]s of at most [chunkSize] bytes each.
     * Every chunk carries the shared [fileHash] and its own [FileChunkPayload.chunkHash].
     */
    fun split(
        fileId: String,
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
        chunkSize: Int = DEFAULT_CHUNK_SIZE,
    ): List<FileChunkPayload> {
        require(chunkSize > 0) { "chunkSize must be positive" }
        val fileHash = sha256(bytes)
        if (bytes.isEmpty()) {
            return listOf(
                FileChunkPayload(
                    fileId = fileId, fileName = fileName, mimeType = mimeType,
                    chunkIndex = 0, totalChunks = 1, fileHash = fileHash,
                    chunkHash = sha256(ByteArray(0)), data = "",
                )
            )
        }
        val total = (bytes.size + chunkSize - 1) / chunkSize
        return (0 until total).map { index ->
            val start = index * chunkSize
            val end = minOf(start + chunkSize, bytes.size)
            val slice = bytes.copyOfRange(start, end)
            FileChunkPayload(
                fileId = fileId,
                fileName = fileName,
                mimeType = mimeType,
                chunkIndex = index,
                totalChunks = total,
                fileHash = fileHash,
                chunkHash = sha256(slice),
                data = encoder.encodeToString(slice),
            )
        }
    }

    /**
     * Reassembles [chunks] (any order) into the original bytes.
     * @throws ReassemblyException if chunks are missing, mismatched, or a hash check fails.
     */
    fun reassemble(chunks: Collection<FileChunkPayload>): ByteArray {
        if (chunks.isEmpty()) throw ReassemblyException("No chunks provided")
        val total = chunks.first().totalChunks
        val fileHash = chunks.first().fileHash

        val byIndex = HashMap<Int, FileChunkPayload>()
        for (c in chunks) {
            if (c.totalChunks != total || c.fileHash != fileHash) {
                throw ReassemblyException("Chunk ${c.chunkIndex} belongs to a different transfer")
            }
            val bytes = decoder.decode(c.data)
            if (sha256(bytes) != c.chunkHash) {
                throw ReassemblyException("Chunk ${c.chunkIndex} failed integrity check")
            }
            byIndex[c.chunkIndex] = c
        }
        if (byIndex.size != total) {
            val missing = (0 until total).filter { it !in byIndex }
            throw ReassemblyException("Missing chunks: $missing")
        }

        val out = java.io.ByteArrayOutputStream()
        for (i in 0 until total) {
            out.write(decoder.decode(byIndex.getValue(i).data))
        }
        val result = out.toByteArray()
        if (sha256(result) != fileHash) {
            throw ReassemblyException("Reassembled file failed hash verification")
        }
        return result
    }

    /** Builds a [FileTransfer] metadata record describing the given chunk set. */
    fun describe(
        chunks: List<FileChunkPayload>,
        senderId: String,
        receiverId: String,
        sizeBytes: Long,
        outgoing: Boolean,
    ): FileTransfer {
        val first = chunks.first()
        return FileTransfer(
            fileId = first.fileId,
            fileName = first.fileName,
            mimeType = first.mimeType,
            sizeBytes = sizeBytes,
            totalChunks = first.totalChunks,
            fileHash = first.fileHash,
            senderId = senderId,
            receiverId = receiverId,
            outgoing = outgoing,
        )
    }

    fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}

/** Raised when file chunks cannot be reassembled into a valid file. */
class ReassemblyException(message: String) : Exception(message)
