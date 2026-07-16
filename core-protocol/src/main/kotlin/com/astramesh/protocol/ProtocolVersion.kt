package com.astramesh.protocol

/**
 * Protocol version constants (see docs/protocol.md §4).
 *
 * Every packet carries [CURRENT] so older nodes can safely reject unsupported packets
 * and the protocol can evolve gradually.
 */
object ProtocolVersion {
    /** Human-readable semantic protocol version. */
    const val CURRENT: String = "1.0.0"

    /** Integer schema version for the packet envelope format. */
    const val PACKET_SCHEMA: Int = 1

    /** Lowest protocol version this build can interoperate with. */
    const val MIN_SUPPORTED: String = "1.0.0"

    /** Returns true if a peer advertising [version] is compatible with this build. */
    fun isSupported(version: String): Boolean = version == CURRENT
}
