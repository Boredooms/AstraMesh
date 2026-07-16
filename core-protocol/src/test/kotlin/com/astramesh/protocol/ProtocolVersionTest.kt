package com.astramesh.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolVersionTest {

    @Test
    fun current_isSupported() {
        assertTrue(ProtocolVersion.isSupported(ProtocolVersion.CURRENT))
    }

    @Test
    fun unknownVersion_isRejected() {
        assertFalse(ProtocolVersion.isSupported("9.9.9"))
    }

    @Test
    fun schemaVersion_isPositive() {
        assertTrue(ProtocolVersion.PACKET_SCHEMA >= 1)
    }

    @Test
    fun priorityOrdering_emergencyOutranksNormalOutranksBulk() {
        assertTrue(Priority.EMERGENCY.weight > Priority.NORMAL.weight)
        assertTrue(Priority.NORMAL.weight > Priority.BULK.weight)
        assertEquals(Priority.NORMAL, Priority.DEFAULT)
    }
}
