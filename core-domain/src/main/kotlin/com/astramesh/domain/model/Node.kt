package com.astramesh.domain.model

/**
 * A device participating in the mesh (docs/protocol.md §5).
 *
 * Identity is generated locally — no online account is required. The node id is stable
 * across app restarts.
 */
data class Node(
    val nodeId: String,
    val deviceName: String,
    val platformType: PlatformType,
    val publicKey: String,
    val keyFingerprint: String,
    val capabilities: Set<Capability>,
    val lastSeen: Long,
    val relayCapable: Boolean = true,
) {
    val supportsRelay: Boolean get() = relayCapable && Capability.RELAY in capabilities
}

enum class PlatformType { ANDROID, DESKTOP, UNKNOWN }

/** Advertised node capabilities exchanged during discovery/handshake (docs/protocol.md §6). */
enum class Capability {
    CHAT,
    RELAY,
    BROADCAST,
    FILE_TRANSFER,
    DESKTOP_VIEW,
}
