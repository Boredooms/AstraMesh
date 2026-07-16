package com.astramesh.domain.model

/**
 * A discovered peer and its current link state (docs/workflow.md §5, §6).
 *
 * [Node] is the persisted identity; [Peer] wraps it with transient discovery/session info
 * that changes as the device moves in and out of range.
 */
data class Peer(
    val node: Node,
    val sessionState: SessionState,
    val signalStrength: Int?,
    val lastContact: Long,
) {
    val nodeId: String get() = node.nodeId
    val isConnected: Boolean get() = sessionState == SessionState.ACTIVE
}

/** Session lifecycle with a peer (docs/workflow.md §6). */
enum class SessionState {
    DISCOVERED,
    HANDSHAKING,
    SECURE,
    ACTIVE,
    INTERRUPTED,
    RETRYING,
    CLOSED,
}
