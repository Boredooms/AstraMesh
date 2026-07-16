package com.astramesh.routing

import com.astramesh.protocol.Packet

/**
 * The decision the routing engine makes for an incoming or outgoing packet
 * (docs/routing.md §3). Pure data — the caller (repository/transport layer) executes it.
 */
sealed interface RoutingDecision {
    /** Packet is a duplicate or expired; do nothing. */
    data class Drop(val reason: DropReason) : RoutingDecision

    /** Packet is addressed to this node; hand it to the app + send an ACK. */
    data class DeliverLocally(val packet: Packet) : RoutingDecision

    /** Destination is a direct peer; forward the (hop-advanced) packet straight to it. */
    data class ForwardDirect(val packet: Packet, val nextHop: String) : RoutingDecision

    /** No direct route; relay the (hop-advanced) packet to these neighbor node ids. */
    data class Relay(val packet: Packet, val neighbors: List<String>) : RoutingDecision

    /** No neighbors available; keep the packet for a later retry (store-and-forward). */
    data class Queue(val packet: Packet) : RoutingDecision
}

enum class DropReason { DUPLICATE, EXPIRED, MALFORMED, UNSUPPORTED_VERSION }
