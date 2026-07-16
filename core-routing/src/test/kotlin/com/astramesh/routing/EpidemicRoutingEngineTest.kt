package com.astramesh.routing

import com.astramesh.protocol.Packet
import com.astramesh.protocol.PacketType
import com.astramesh.protocol.Priority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EpidemicRoutingEngineTest {

    private val local = "node-self"

    private fun packet(
        id: String = "p1",
        receiver: String = "node-b",
        sender: String = "node-a",
        ttl: Int = 5,
        type: PacketType = PacketType.CHAT,
    ) = Packet(
        packetId = id,
        type = type,
        senderId = sender,
        receiverId = receiver,
        timestamp = 1_700_000_000_000,
        ttl = ttl,
        payload = "enc",
        priority = if (type == PacketType.BROADCAST) Priority.EMERGENCY else Priority.NORMAL,
    )

    private fun ctx(
        direct: Set<String> = emptySet(),
        relay: Set<String> = emptySet(),
        now: Long = 1_700_000_000_000,
    ) = RoutingContext(localNodeId = local, directPeers = direct, relayPeers = relay, now = now)

    @Test
    fun duplicatePacket_isDropped() {
        val engine = EpidemicRoutingEngine()
        val p = packet()
        engine.route(p, ctx(relay = setOf("r1")))
        val second = engine.route(p, ctx(relay = setOf("r1")))
        assertEquals(RoutingDecision.Drop(DropReason.DUPLICATE), second)
    }

    @Test
    fun expiredPacket_isDropped() {
        val engine = EpidemicRoutingEngine()
        val decision = engine.route(packet(ttl = 0), ctx(relay = setOf("r1")))
        assertEquals(RoutingDecision.Drop(DropReason.EXPIRED), decision)
    }

    @Test
    fun unsupportedVersion_isDropped() {
        val engine = EpidemicRoutingEngine()
        val p = packet().copy(protocolVersion = "0.0.1")
        val decision = engine.route(p, ctx())
        assertEquals(RoutingDecision.Drop(DropReason.UNSUPPORTED_VERSION), decision)
    }

    @Test
    fun packetForThisNode_isDeliveredLocally() {
        val engine = EpidemicRoutingEngine()
        val p = packet(receiver = local)
        val decision = engine.route(p, ctx())
        assertTrue(decision is RoutingDecision.DeliverLocally)
        assertEquals(p, (decision as RoutingDecision.DeliverLocally).packet)
    }

    @Test
    fun directPeer_isForwardedDirectly_withHopAdvance() {
        val engine = EpidemicRoutingEngine()
        val p = packet(receiver = "node-b", ttl = 5)
        val decision = engine.route(p, ctx(direct = setOf("node-b")))
        assertTrue(decision is RoutingDecision.ForwardDirect)
        decision as RoutingDecision.ForwardDirect
        assertEquals("node-b", decision.nextHop)
        assertEquals(4, decision.packet.ttl)
        assertEquals(1, decision.packet.hopCount)
    }

    @Test
    fun noDirectRoute_relaysToRelayPeers_excludingSender() {
        val engine = EpidemicRoutingEngine()
        val p = packet(sender = "node-a", receiver = "node-z", ttl = 5)
        val decision = engine.route(p, ctx(relay = setOf("node-a", "r1", "r2")))
        assertTrue(decision is RoutingDecision.Relay)
        decision as RoutingDecision.Relay
        // sender excluded to avoid bouncing straight back
        assertEquals(setOf("r1", "r2"), decision.neighbors.toSet())
        assertEquals(4, decision.packet.ttl)
    }

    @Test
    fun noReachablePeers_queuesForStoreAndForward() {
        val engine = EpidemicRoutingEngine()
        val decision = engine.route(packet(receiver = "node-z"), ctx())
        assertTrue(decision is RoutingDecision.Queue)
    }

    @Test
    fun broadcast_isRelayedToAllRelayPeers() {
        val engine = EpidemicRoutingEngine()
        val p = packet(
            id = "b1",
            receiver = Packet.BROADCAST_RECEIVER,
            sender = "node-a",
            type = PacketType.BROADCAST,
        )
        val decision = engine.route(p, ctx(relay = setOf("r1", "r2")))
        assertTrue(decision is RoutingDecision.Relay)
        assertEquals(setOf("r1", "r2"), (decision as RoutingDecision.Relay).neighbors.toSet())
    }

    @Test
    fun relayWithLastTtl_dropsInsteadOfNegativeTtl() {
        val engine = EpidemicRoutingEngine()
        // ttl=1: still relayable once (canRelay true), becomes ttl 0 after hop.
        val p = packet(receiver = "node-z", ttl = 1)
        val decision = engine.route(p, ctx(relay = setOf("r1")))
        assertTrue(decision is RoutingDecision.Relay)
        assertEquals(0, (decision as RoutingDecision.Relay).packet.ttl)
    }
}
