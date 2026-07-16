package com.astramesh.domain.model

/**
 * A high-priority emergency / community broadcast (docs/workflow.md §11).
 * Broadcasts flood the mesh and are deduplicated globally by [packetId].
 */
data class Broadcast(
    val id: String,
    val packetId: String,
    val senderId: String,
    val text: String,
    val severity: Severity,
    val timestamp: Long,
    val expiresAt: Long? = null,
    val outgoing: Boolean,
    val hopCount: Int = 0,
) {
    enum class Severity { INFO, WARNING, CRITICAL }

    fun isExpired(now: Long): Boolean = expiresAt != null && now >= expiresAt
}
