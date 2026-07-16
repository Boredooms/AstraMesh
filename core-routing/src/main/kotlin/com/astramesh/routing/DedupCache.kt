package com.astramesh.routing

/**
 * Bounded, time-windowed cache of seen packet ids for deduplication
 * (docs/protocol.md §14, docs/routing.md §7).
 *
 * Implementations must be safe to call from the routing engine and keep memory bounded by
 * evicting the oldest entries once capacity or the retention window is exceeded.
 */
interface DedupCache {
    /**
     * Records [packetId] as seen at [now] (epoch millis).
     * @return true if this id was newly recorded, false if it was already present (a duplicate).
     */
    fun markSeen(packetId: String, now: Long): Boolean

    /** True if [packetId] has been seen within the retention window. */
    fun hasSeen(packetId: String): Boolean

    /** Current number of retained ids. */
    val size: Int

    fun clear()
}
