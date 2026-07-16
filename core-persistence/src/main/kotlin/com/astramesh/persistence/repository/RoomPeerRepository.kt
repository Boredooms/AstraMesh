package com.astramesh.persistence.repository

import com.astramesh.domain.model.Node
import com.astramesh.domain.model.Peer
import com.astramesh.domain.repository.PeerRepository
import com.astramesh.persistence.dao.NodeDao
import com.astramesh.persistence.repository.Mappers.toEntity
import com.astramesh.persistence.repository.Mappers.toNode
import com.astramesh.persistence.repository.Mappers.toPeerOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomPeerRepository @Inject constructor(
    private val dao: NodeDao,
) : PeerRepository {

    override fun observePeers(): Flow<List<Peer>> =
        dao.observePeers().map { list -> list.mapNotNull { it.toPeerOrNull() } }

    override suspend fun getNode(nodeId: String): Node? = dao.getNode(nodeId)?.toNode()

    override suspend fun upsertNode(node: Node) {
        // Preserve any existing session/link state when only the identity changes.
        val existing = dao.getNode(node.nodeId)
        dao.upsert(
            node.toEntity(
                sessionState = existing?.sessionState,
                signalStrength = existing?.signalStrength,
                lastContact = existing?.lastContact,
            )
        )
    }

    override suspend fun upsertPeer(peer: Peer) = dao.upsert(peer.toEntity())

    override suspend fun removePeer(nodeId: String) = dao.delete(nodeId)
}
