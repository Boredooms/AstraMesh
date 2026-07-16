package com.astramesh.routing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryDedupCacheTest {

    @Test
    fun firstSight_isNew_secondSight_isDuplicate() {
        val cache = InMemoryDedupCache()
        assertTrue(cache.markSeen("a", 0))
        assertFalse(cache.markSeen("a", 1))
        assertTrue(cache.hasSeen("a"))
    }

    @Test
    fun distinctIds_areIndependent() {
        val cache = InMemoryDedupCache()
        assertTrue(cache.markSeen("a", 0))
        assertTrue(cache.markSeen("b", 0))
        assertEquals(2, cache.size)
    }

    @Test
    fun expiredEntries_arePurged_andCanBeSeenAgain() {
        val cache = InMemoryDedupCache(retentionMillis = 100)
        cache.markSeen("a", 0)
        // 200ms later, "a" is beyond retention. A new write triggers eviction.
        assertTrue(cache.markSeen("b", 200))
        assertFalse(cache.hasSeen("a"))
        // "a" seen again is treated as new.
        assertTrue(cache.markSeen("a", 200))
    }

    @Test
    fun overflow_evictsOldestFirst() {
        val cache = InMemoryDedupCache(maxEntries = 3, retentionMillis = Long.MAX_VALUE)
        cache.markSeen("a", 1)
        cache.markSeen("b", 2)
        cache.markSeen("c", 3)
        cache.markSeen("d", 4) // evicts "a"
        assertFalse(cache.hasSeen("a"))
        assertTrue(cache.hasSeen("b"))
        assertTrue(cache.hasSeen("d"))
        assertEquals(3, cache.size)
    }

    @Test
    fun clear_empties() {
        val cache = InMemoryDedupCache()
        cache.markSeen("a", 0)
        cache.clear()
        assertEquals(0, cache.size)
        assertFalse(cache.hasSeen("a"))
    }
}
