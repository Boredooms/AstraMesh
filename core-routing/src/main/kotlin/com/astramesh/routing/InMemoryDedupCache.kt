package com.astramesh.routing

/**
 * Bounded, time-windowed [DedupCache] backed by a LinkedHashMap in insertion order
 * (docs/routing.md §7). Not thread-safe; the routing engine is expected to serialize access.
 *
 * Eviction happens on write: entries older than [retentionMillis] are purged, and if the map
 * still exceeds [maxEntries] the oldest entries are dropped until it fits.
 *
 * @param maxEntries hard cap on retained ids (memory safety)
 * @param retentionMillis how long an id is remembered before it may be re-processed
 */
class InMemoryDedupCache(
    private val maxEntries: Int = 4096,
    private val retentionMillis: Long = 10 * 60 * 1000L,
) : DedupCache {

    // packetId -> timestamp seen; access-order false so eldest = oldest insertion.
    private val seen = LinkedHashMap<String, Long>()

    override fun markSeen(packetId: String, now: Long): Boolean {
        evictExpired(now)
        val isNew = !seen.containsKey(packetId)
        // Re-insert to move to the end (most recent) and refresh timestamp.
        seen.remove(packetId)
        seen[packetId] = now
        evictOverflow()
        return isNew
    }

    override fun hasSeen(packetId: String): Boolean = seen.containsKey(packetId)

    override val size: Int get() = seen.size

    override fun clear() = seen.clear()

    private fun evictExpired(now: Long) {
        val cutoff = now - retentionMillis
        val it = seen.entries.iterator()
        while (it.hasNext()) {
            val entry = it.next()
            // Insertion order => once we hit a fresh entry, the rest are fresher too.
            if (entry.value < cutoff) it.remove() else break
        }
    }

    private fun evictOverflow() {
        while (seen.size > maxEntries) {
            val eldest = seen.keys.iterator().next()
            seen.remove(eldest)
        }
    }
}
