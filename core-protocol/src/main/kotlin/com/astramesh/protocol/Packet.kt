package com.astramesh.protocol

import kotlinx.serialization.Serializable

/**
 * The common envelope shared by all AstraMesh packets (docs/protocol.md §8).
 *
 * Routing fields ([packetId], [type], [senderId], [receiverId], [ttl], [hopCount],
 * [priority]) are visible to relay nodes so they can forward without decrypting. The
 * [payload] is opaque, already-encrypted content — relays MUST NOT be able to read it
 * (docs/protocol.md §12).
 *
 * This type is transport-agnostic and pure Kotlin so it can be unit-tested on the JVM and
 * reused by the optional desktop companion.
 *
 * @property protocolVersion top-level protocol compatibility string, e.g. "1.0.0"
 * @property packetVersion integer schema version of this envelope format
 * @property packetId globally-unique id used for deduplication and ACK targeting
 * @property type packet category
 * @property senderId origin node id
 * @property receiverId destination node id, or [BROADCAST_RECEIVER] for a broadcast
 * @property timestamp send time in epoch milliseconds
 * @property ttl remaining hops the packet may still traverse
 * @property hopCount number of relays already traversed
 * @property priority routing priority
 * @property payload opaque, encrypted content (Base64 or transport-native bytes as text)
 * @property signature optional authenticity signature over the envelope
 * @property checksum optional integrity checksum of the payload
 */
@Serializable
data class Packet(
    val protocolVersion: String = ProtocolVersion.CURRENT,
    val packetVersion: Int = ProtocolVersion.PACKET_SCHEMA,
    val packetId: String,
    val type: PacketType,
    val senderId: String,
    val receiverId: String,
    val timestamp: Long,
    val ttl: Int,
    val hopCount: Int = 0,
    val priority: Priority = Priority.DEFAULT,
    val payload: String,
    val signature: String? = null,
    val checksum: String? = null,
) {
    /** True if this packet is addressed to every reachable node rather than one receiver. */
    val isBroadcast: Boolean get() = receiverId == BROADCAST_RECEIVER

    /** True if the packet can no longer be relayed (TTL exhausted). */
    val isExpired: Boolean get() = ttl <= 0

    /**
     * Returns a copy advanced by one hop: [hopCount] incremented and [ttl] decremented.
     * Callers should check [canRelay] first.
     */
    fun relayed(): Packet = copy(hopCount = hopCount + 1, ttl = ttl - 1)

    /** True if this packet still has TTL budget to be forwarded another hop. */
    val canRelay: Boolean get() = ttl > 0

    companion object {
        /** Sentinel [receiverId] marking a broadcast packet. */
        const val BROADCAST_RECEIVER: String = "*"

        /** Default TTL (max hops) for a freshly created packet. */
        const val DEFAULT_TTL: Int = 8
    }
}
