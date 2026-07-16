package com.astramesh.mesh

import com.astramesh.domain.identity.NodeIdentityProvider
import com.astramesh.domain.model.Broadcast
import com.astramesh.domain.model.DeliveryState
import com.astramesh.domain.model.Message
import com.astramesh.domain.model.Node
import com.astramesh.domain.model.Peer
import com.astramesh.domain.repository.MessageRepository
import com.astramesh.domain.repository.PeerRepository
import com.astramesh.domain.repository.RelayQueueRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap

/** In-memory [MessageRepository] for JVM tests (Room needs instrumentation). */
class FakeMessageRepository : MessageRepository {
    private val store = MutableStateFlow<List<Message>>(emptyList())

    override fun observeConversation(peerId: String): Flow<List<Message>> =
        store.map { list -> list.filter { it.senderId == peerId || it.receiverId == peerId } }

    override fun observeConversations(): Flow<List<Message>> = store

    override suspend fun getByPacketId(packetId: String): Message? =
        store.value.firstOrNull { it.packetId == packetId }

    override suspend fun upsert(message: Message) {
        store.value = store.value.filterNot { it.packetId == message.packetId } + message
    }

    override suspend fun updateState(packetId: String, state: DeliveryState) {
        store.value = store.value.map { if (it.packetId == packetId) it.copy(state = state) else it }
    }

    override suspend fun pending(): List<Message> =
        store.value.filter { it.state == DeliveryState.PENDING }

    override fun observePendingCount(): Flow<Int> = store.map { list ->
        list.count { it.state in setOf(DeliveryState.PENDING, DeliveryState.SENT, DeliveryState.RELAYED) }
    }

    override suspend fun incrementRetryCount(packetId: String) {
        store.value = store.value.map {
            if (it.packetId == packetId) it.copy(retryCount = it.retryCount + 1) else it
        }
    }

    fun snapshot(): List<Message> = store.value
}

/** In-memory [PeerRepository] for JVM tests. */
class FakePeerRepository : PeerRepository {
    private val nodes = ConcurrentHashMap<String, Node>()
    private val peers = MutableStateFlow<List<Peer>>(emptyList())

    override fun observePeers(): Flow<List<Peer>> = peers

    override suspend fun getNode(nodeId: String): Node? = nodes[nodeId]

    override suspend fun upsertNode(node: Node) {
        nodes[node.nodeId] = node
    }

    override suspend fun upsertPeer(peer: Peer) {
        nodes[peer.nodeId] = peer.node
        peers.value = peers.value.filterNot { it.nodeId == peer.nodeId } + peer
    }

    override suspend fun removePeer(nodeId: String) {
        peers.value = peers.value.filterNot { it.nodeId == nodeId }
    }
}

/** In-memory [RelayQueueRepository] for JVM tests. */
class FakeRelayQueueRepository : RelayQueueRepository {
    private val queue = MutableStateFlow<List<com.astramesh.protocol.Packet>>(emptyList())

    override fun observeQueueSize(): Flow<Int> = queue.map { it.size }

    override suspend fun enqueue(packet: com.astramesh.protocol.Packet) {
        queue.value = queue.value.filterNot { it.packetId == packet.packetId } + packet
    }

    override suspend fun all(): List<com.astramesh.protocol.Packet> = queue.value

    override suspend fun remove(packetId: String) {
        queue.value = queue.value.filterNot { it.packetId == packetId }
    }
}

/** Fixed identity for tests. */
class FakeIdentity(
    private val id: String,
    private val node: Node,
    private val priv: String,
) : NodeIdentityProvider {
    override suspend fun nodeId(): String = id
    override suspend fun localNode(): Node = node
    override suspend fun privateKey(): String = priv
}
