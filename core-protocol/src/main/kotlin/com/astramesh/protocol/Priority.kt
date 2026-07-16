package com.astramesh.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Routing priority for a packet (docs/protocol.md §13, docs/routing.md §5).
 * Higher [weight] is forwarded ahead of lower.
 */
@Serializable
enum class Priority(val weight: Int) {
    @SerialName("emergency")
    EMERGENCY(3),

    @SerialName("normal")
    NORMAL(2),

    @SerialName("bulk")
    BULK(1);

    companion object {
        val DEFAULT = NORMAL
    }
}
