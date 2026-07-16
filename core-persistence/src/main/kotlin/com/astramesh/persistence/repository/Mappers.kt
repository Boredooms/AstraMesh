package com.astramesh.persistence.repository

import com.astramesh.domain.model.Broadcast
import com.astramesh.domain.model.Capability
import com.astramesh.domain.model.DeliveryState
import com.astramesh.domain.model.FileTransfer
import com.astramesh.domain.model.Message
import com.astramesh.domain.model.Node
import com.astramesh.domain.model.Peer
import com.astramesh.domain.model.PlatformType
import com.astramesh.domain.model.SessionState
import com.astramesh.persistence.entity.BroadcastEntity
import com.astramesh.persistence.entity.FileTransferEntity
import com.astramesh.persistence.entity.MessageEntity
import com.astramesh.persistence.entity.NodeEntity

/**
 * Pure functions converting between Room entities and domain models. Kept separate from the
 * repositories so mapping is trivially testable and the domain stays persistence-agnostic.
 */
internal object Mappers {

    // ---- Node / Peer ----

    fun NodeEntity.toNode(): Node = Node(
        nodeId = nodeId,
        deviceName = deviceName,
        platformType = runCatching { PlatformType.valueOf(platformType) }
            .getOrDefault(PlatformType.UNKNOWN),
        publicKey = publicKey,
        keyFingerprint = keyFingerprint,
        capabilities = capabilities.split(",")
            .filter { it.isNotBlank() }
            .mapNotNull { name -> runCatching { Capability.valueOf(name) }.getOrNull() }
            .toSet(),
        lastSeen = lastSeen,
        relayCapable = relayCapable,
    )

    fun Node.toEntity(
        sessionState: String? = null,
        signalStrength: Int? = null,
        lastContact: Long? = null,
    ): NodeEntity = NodeEntity(
        nodeId = nodeId,
        deviceName = deviceName,
        platformType = platformType.name,
        publicKey = publicKey,
        keyFingerprint = keyFingerprint,
        capabilities = capabilities.joinToString(",") { it.name },
        relayCapable = relayCapable,
        lastSeen = lastSeen,
        sessionState = sessionState,
        signalStrength = signalStrength,
        lastContact = lastContact,
    )

    fun NodeEntity.toPeerOrNull(): Peer? {
        val state = sessionState?.let {
            runCatching { SessionState.valueOf(it) }.getOrNull()
        } ?: return null
        return Peer(
            node = toNode(),
            sessionState = state,
            signalStrength = signalStrength,
            lastContact = lastContact ?: lastSeen,
        )
    }

    fun Peer.toEntity(): NodeEntity = node.toEntity(
        sessionState = sessionState.name,
        signalStrength = signalStrength,
        lastContact = lastContact,
    )

    // ---- Message ----

    fun MessageEntity.toMessage(): Message = Message(
        id = id,
        packetId = packetId,
        senderId = senderId,
        receiverId = receiverId,
        text = text,
        timestamp = timestamp,
        state = runCatching { DeliveryState.valueOf(state) }.getOrDefault(DeliveryState.PENDING),
        outgoing = outgoing,
        hopCount = hopCount,
        replyToId = replyToId,
    )

    fun Message.toEntity(): MessageEntity {
        val peer = if (outgoing) receiverId else senderId
        return MessageEntity(
            id = id,
            packetId = packetId,
            senderId = senderId,
            receiverId = receiverId,
            peerId = peer,
            text = text,
            timestamp = timestamp,
            state = state.name,
            outgoing = outgoing,
            hopCount = hopCount,
            replyToId = replyToId,
        )
    }

    // ---- Broadcast ----

    fun BroadcastEntity.toBroadcast(): Broadcast = Broadcast(
        id = id,
        packetId = packetId,
        senderId = senderId,
        text = text,
        severity = runCatching { Broadcast.Severity.valueOf(severity) }
            .getOrDefault(Broadcast.Severity.INFO),
        timestamp = timestamp,
        expiresAt = expiresAt,
        outgoing = outgoing,
        hopCount = hopCount,
    )

    fun Broadcast.toEntity(): BroadcastEntity = BroadcastEntity(
        id = id,
        packetId = packetId,
        senderId = senderId,
        text = text,
        severity = severity.name,
        timestamp = timestamp,
        expiresAt = expiresAt,
        outgoing = outgoing,
        hopCount = hopCount,
    )

    // ---- FileTransfer ----

    fun FileTransferEntity.toTransfer(): FileTransfer = FileTransfer(
        fileId = fileId,
        fileName = fileName,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        totalChunks = totalChunks,
        fileHash = fileHash,
        senderId = senderId,
        receiverId = receiverId,
        outgoing = outgoing,
        receivedChunks = receivedChunks.split(",")
            .filter { it.isNotBlank() }
            .map { it.toInt() }
            .toSet(),
        state = runCatching { DeliveryState.valueOf(state) }.getOrDefault(DeliveryState.PENDING),
    )

    fun FileTransfer.toEntity(): FileTransferEntity = FileTransferEntity(
        fileId = fileId,
        fileName = fileName,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        totalChunks = totalChunks,
        fileHash = fileHash,
        senderId = senderId,
        receiverId = receiverId,
        outgoing = outgoing,
        receivedChunks = receivedChunks.sorted().joinToString(","),
        state = state.name,
    )
}
