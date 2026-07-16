package com.astramesh.routing

import com.astramesh.protocol.Packet
import com.astramesh.protocol.ProtocolVersion

/**
 * MVP epidemic-relay [RoutingEngine] (docs/routing.md §2–3).
 *
 * Decision order for an incoming packet:
 * 1. reject unsupported protocol versions        -> Drop(UNSUPPORTED_VERSION)
 * 2. drop duplicates (dedup cache)               -> Drop(DUPLICATE)
 * 3. drop expired packets (TTL exhausted)        -> Drop(EXPIRED)
 * 4. addressed to this node                       -> DeliverLocally
 * 5. destination is a direct peer                 -> ForwardDirect (hop-advanced)
 * 6. relay peers available                        -> Relay to them (hop-advanced)
 * 7. nothing reachable                            -> Queue (store-and-forward)
 *
 * Pure and deterministic given the [RoutingContext]; the dedup cache is the only mutable state.
 * Broadcasts are always relayed to every relay peer (never "delivered only") so they keep
 * flooding, while still being deduplicated by packet id.
 */
class EpidemicRoutingEngine(
    private val dedupCache: DedupCache = InMemoryDedupCache(),
) : RoutingEngine {

    override val dedupCacheSize: Int get() = dedupCache.size

    override fun route(incoming: Packet, context: RoutingContext): RoutingDecision {
        if (!ProtocolVersion.isSupported(incoming.protocolVersion)) {
            return RoutingDecision.Drop(DropReason.UNSUPPORTED_VERSION)
        }

        // Deduplicate first: a duplicate is dropped regardless of destination.
        val isNew = dedupCache.markSeen(incoming.packetId, context.now)
        if (!isNew) {
            return RoutingDecision.Drop(DropReason.DUPLICATE)
        }

        if (incoming.isExpired) {
            return RoutingDecision.Drop(DropReason.EXPIRED)
        }

        // Broadcasts: deliver locally AND keep flooding. We surface local delivery here; the
        // caller re-injects for relay. To keep the decision single-purpose, a broadcast that
        // still has TTL is relayed to all relay peers; the caller delivers it locally too.
        if (incoming.isBroadcast) {
            return relayOrQueue(incoming, context)
        }

        // Addressed to us: deliver and let the caller emit an ACK.
        if (incoming.receiverId == context.localNodeId) {
            return RoutingDecision.DeliverLocally(incoming)
        }

        // Destination directly reachable: forward straight to it.
        if (incoming.receiverId in context.directPeers) {
            if (!incoming.canRelay) return RoutingDecision.Drop(DropReason.EXPIRED)
            return RoutingDecision.ForwardDirect(incoming.relayed(), incoming.receiverId)
        }

        // Otherwise relay epidemically or store for later.
        return relayOrQueue(incoming, context)
    }

    private fun relayOrQueue(incoming: Packet, context: RoutingContext): RoutingDecision {
        if (!incoming.canRelay) return RoutingDecision.Drop(DropReason.EXPIRED)

        // Relay to every relay-capable neighbor except the node we received it from.
        val targets = context.relayPeers.filter { it != incoming.senderId }
        return if (targets.isEmpty()) {
            RoutingDecision.Queue(incoming)
        } else {
            RoutingDecision.Relay(incoming.relayed(), targets)
        }
    }
}
