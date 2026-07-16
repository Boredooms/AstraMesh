package com.astramesh.domain.repository

import com.astramesh.domain.model.Node
import com.astramesh.domain.model.Peer
import kotlinx.coroutines.flow.Flow

/**
 * Persistence + retrieval of known nodes and live peers (docs/architecture.md §13).
 */
interface PeerRepository {
    /** Currently discovered peers with live link state, as a reactive stream. */
    fun observePeers(): Flow<List<Peer>>

    suspend fun getNode(nodeId: String): Node?

    /** Record or refresh a known node identity. */
    suspend fun upsertNode(node: Node)

    /** Update the transient discovery/session state for a peer. */
    suspend fun upsertPeer(peer: Peer)

    suspend fun removePeer(nodeId: String)
}
