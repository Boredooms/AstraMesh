package com.astramesh.domain.file

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FileChunkerTest {

    private fun bytes(n: Int): ByteArray = ByteArray(n) { (it % 251).toByte() }

    @Test
    fun split_thenReassemble_restoresOriginal() {
        val data = bytes(10_000)
        val chunks = FileChunker.split("f1", "photo.jpg", "image/jpeg", data, chunkSize = 4096)
        assertEquals(3, chunks.size)
        assertEquals(3, chunks.first().totalChunks)
        val restored = FileChunker.reassemble(chunks)
        assertArrayEquals(data, restored)
    }

    @Test
    fun reassemble_worksOutOfOrder() {
        val data = bytes(9000)
        val chunks = FileChunker.split("f", "a.bin", "application/octet-stream", data, 4096)
        val restored = FileChunker.reassemble(chunks.shuffled())
        assertArrayEquals(data, restored)
    }

    @Test
    fun exactMultipleOfChunkSize() {
        val data = bytes(8192)
        val chunks = FileChunker.split("f", "a", "text/plain", data, 4096)
        assertEquals(2, chunks.size)
        assertArrayEquals(data, FileChunker.reassemble(chunks))
    }

    @Test
    fun emptyFile_yieldsSingleChunk() {
        val chunks = FileChunker.split("f", "empty.txt", "text/plain", ByteArray(0))
        assertEquals(1, chunks.size)
        assertArrayEquals(ByteArray(0), FileChunker.reassemble(chunks))
    }

    @Test
    fun missingChunk_isDetected() {
        val chunks = FileChunker.split("f", "a", "text/plain", bytes(9000), 4096)
        val missingOne = chunks.drop(1)
        val ex = assertThrows(ReassemblyException::class.java) {
            FileChunker.reassemble(missingOne)
        }
        assertTrue(ex.message!!.contains("Missing"))
    }

    @Test
    fun tamperedChunk_failsIntegrity() {
        val chunks = FileChunker.split("f", "a", "text/plain", bytes(5000), 4096).toMutableList()
        val bad = chunks[0].copy(data = java.util.Base64.getEncoder().encodeToString(bytes(4096) + 1))
        chunks[0] = bad
        assertThrows(ReassemblyException::class.java) {
            FileChunker.reassemble(chunks)
        }
    }

    @Test
    fun mixedTransfers_areRejected() {
        val a = FileChunker.split("fa", "a", "text/plain", bytes(5000), 4096)
        val b = FileChunker.split("fb", "b", "text/plain", bytes(5000), 4096)
        assertThrows(ReassemblyException::class.java) {
            FileChunker.reassemble(listOf(a.first(), b.first()))
        }
    }

    @Test
    fun describe_buildsMetadata() {
        val data = bytes(9000)
        val chunks = FileChunker.split("f", "doc.pdf", "application/pdf", data, 4096)
        val ft = FileChunker.describe(chunks, "alice", "bob", data.size.toLong(), outgoing = true)
        assertEquals("f", ft.fileId)
        assertEquals(3, ft.totalChunks)
        assertEquals(9000L, ft.sizeBytes)
        assertTrue(ft.outgoing)
    }
}
