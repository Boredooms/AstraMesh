package com.astramesh.domain.model

/**
 * Delivery lifecycle of a message or packet (docs/protocol.md §10, docs/workflow.md §7).
 *
 * ```
 * DRAFT -> PENDING -> SENT -> RELAYED -> DELIVERED
 *                       \-> FAILED
 *                       \-> EXPIRED
 * ```
 */
enum class DeliveryState {
    /** Composed but not yet queued for send. */
    DRAFT,

    /** Persisted and waiting for a send opportunity (store-and-forward). */
    PENDING,

    /** Handed to the transport layer. */
    SENT,

    /** Observed being relayed by at least one neighbor. */
    RELAYED,

    /** Confirmed received by the destination (ACK returned). */
    DELIVERED,

    /** Exceeded retry budget without delivery. */
    FAILED,

    /** TTL / time budget elapsed before delivery. */
    EXPIRED;

    val isTerminal: Boolean get() = this == DELIVERED || this == FAILED || this == EXPIRED
}
