package com.astramesh.mesh

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicLong

/** Point-in-time snapshot of mesh activity, for the diagnostics screen (Settings → Diagnostics). */
data class PacketCounterSnapshot(
    val chatSent: Long = 0,
    val chatReceived: Long = 0,
    val relayed: Long = 0,
    val acksSent: Long = 0,
    val acksReceived: Long = 0,
    val handshakesSent: Long = 0,
    val handshakesReceived: Long = 0,
)

/**
 * Process-lifetime counters of mesh traffic (docs milestone: "packet counters" diagnostics).
 * A single Hilt singleton instance is shared by [MeshCoordinator] (which increments it) and
 * the diagnostics screen (which observes it) — no persistence needed, these reset on restart
 * by design, they describe the current session's activity.
 */
class PacketCounters {
    private val chatSent = AtomicLong()
    private val chatReceived = AtomicLong()
    private val relayed = AtomicLong()
    private val acksSent = AtomicLong()
    private val acksReceived = AtomicLong()
    private val handshakesSent = AtomicLong()
    private val handshakesReceived = AtomicLong()

    private val _snapshot = MutableStateFlow(PacketCounterSnapshot())
    val snapshot: StateFlow<PacketCounterSnapshot> = _snapshot

    fun onChatSent() = bump { chatSent.incrementAndGet() }
    fun onChatReceived() = bump { chatReceived.incrementAndGet() }
    fun onRelayed() = bump { relayed.incrementAndGet() }
    fun onAckSent() = bump { acksSent.incrementAndGet() }
    fun onAckReceived() = bump { acksReceived.incrementAndGet() }
    fun onHandshakeSent() = bump { handshakesSent.incrementAndGet() }
    fun onHandshakeReceived() = bump { handshakesReceived.incrementAndGet() }

    private inline fun bump(op: () -> Unit) {
        op()
        _snapshot.value = PacketCounterSnapshot(
            chatSent = chatSent.get(),
            chatReceived = chatReceived.get(),
            relayed = relayed.get(),
            acksSent = acksSent.get(),
            acksReceived = acksReceived.get(),
            handshakesSent = handshakesSent.get(),
            handshakesReceived = handshakesReceived.get(),
        )
    }
}
