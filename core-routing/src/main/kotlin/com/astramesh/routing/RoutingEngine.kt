package com.astramesh.routing

import com.astramesh.protocol.Packet

/**
 * Snapshot of local knowledge the [RoutingEngine] needs to decide a packet's next hop
 * (docs/routing.md §4). Supplied by the caller so the engine stays pure and testable.
 *
 * @property localNodeId this device's node id
 * @property directPeers node ids currently reachable over a direct link
 * @property relayPeers subset of reachable node ids that advertise relay capability
 * @property now current time in epoch millis
 */
data class RoutingContext(
    val localNodeId: String,
    val directPeers: Set<String>,
    val relayPeers: Set<String>,
    val now: Long,
)

/**
 * Pure routing/relay engine (docs/routing.md §2–3). No Android, transport, or persistence
 * dependencies — it takes a [Packet] and a [RoutingContext] and returns a [RoutingDecision].
 *
 * The MVP implements epidemic relay with deduplication, TTL enforcement, direct delivery,
 * and store-and-forward queueing.
 */
interface RoutingEngine {
    /**
     * Decides what to do with an [incoming] packet given current [context].
     * The engine also updates its dedup cache as a side effect.
     */
    fun route(incoming: Packet, context: RoutingContext): RoutingDecision
}
