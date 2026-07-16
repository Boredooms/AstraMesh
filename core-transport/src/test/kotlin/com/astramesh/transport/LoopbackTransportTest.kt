package com.astramesh.transport

import com.astramesh.protocol.Packet
import com.astramesh.protocol.PacketType
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoopbackTransportTest {

    private fun packet(id: String, to: String, from: String) = Packet(
        packetId = id,
        type = PacketType.CHAT,
        senderId = from,
        receiverId = to,
        timestamp = 1L,
        ttl = 5,
        payload = "enc",
    )

    @Test
    fun peerDiscoversExistingNode_onStart() = runTest {
        val bus = LoopbackTransport.Bus()
        val a = LoopbackTransport(bus)
        val b = LoopbackTransport(bus)

        a.start("node-a")
        val discovered = async(start = CoroutineStart.UNDISPATCHED) {
            b.events.first { it is TransportEvent.PeerDiscovered } as TransportEvent.PeerDiscovered
        }
        b.start("node-b")
        assertEquals("node-a", discovered.await().endpoint.nodeId)
    }

    @Test
    fun sendDeliversPacketToAddressedPeer() = runTest {
        val bus = LoopbackTransport.Bus()
        val a = LoopbackTransport(bus)
        val b = LoopbackTransport(bus)
        a.start("node-a")
        b.start("node-b")

        val received = async(start = CoroutineStart.UNDISPATCHED) {
            b.events.first { it is TransportEvent.PacketReceived } as TransportEvent.PacketReceived
        }
        val endpointB = PeerEndpoint("node-b", "node-b", TransportKind.LOOPBACK)
        val ok = a.send(packet("p1", to = "node-b", from = "node-a"), endpointB)

        assertTrue(ok)
        val evt = received.await()
        assertEquals("p1", evt.packet.packetId)
        assertEquals("node-a", evt.from.nodeId)
    }

    @Test
    fun sendToUnknownPeer_fails() = runTest {
        val bus = LoopbackTransport.Bus()
        val a = LoopbackTransport(bus)
        a.start("node-a")
        val ghost = PeerEndpoint("ghost", "ghost", TransportKind.LOOPBACK)
        assertFalse(a.send(packet("p", "ghost", "node-a"), ghost))
    }

    @Test
    fun knownEndpoints_excludeSelf() = runTest {
        val bus = LoopbackTransport.Bus()
        val a = LoopbackTransport(bus)
        val b = LoopbackTransport(bus)
        a.start("node-a")
        b.start("node-b")
        val known = a.knownEndpoints().map { it.nodeId }
        assertTrue(known.contains("node-b"))
        assertFalse(known.contains("node-a"))
    }
}
