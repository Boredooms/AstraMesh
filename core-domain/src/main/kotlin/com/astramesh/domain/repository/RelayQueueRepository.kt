package com.astramesh.domain.repository

import com.astramesh.protocol.Packet
import kotlinx.coroutines.flow.Flow

/**
 * Store-and-forward queue for packets this node is relaying but could not yet forward
 * (docs/protocol.md §20 table `packet_cache`, docs/routing.md §6).
 *
 * Distinct from [MessageRepository]: this holds opaque, still-encrypted [Packet]s that this
 * node is carrying *for other nodes* — it cannot and does not decrypt them. A node's own
 * outgoing messages are retried from [MessageRepository.pending] instead.
 */
interface RelayQueueRepository {
    /** Reactive count of currently queued packets, for diagnostics. */
    fun observeQueueSize(): Flow<Int>

    /** Persists [packet] for a later forward attempt when a route becomes available. */
    suspend fun enqueue(packet: Packet)

    /** All currently queued packets, oldest first. */
    suspend fun all(): List<Packet>

    /** Removes a packet once it has been forwarded or has expired. */
    suspend fun remove(packetId: String)
}
