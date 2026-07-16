package com.astramesh.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astramesh.domain.model.Peer
import com.astramesh.domain.repository.PeerRepository
import com.astramesh.domain.repository.RelayQueueRepository
import com.astramesh.mesh.PacketCounterSnapshot
import com.astramesh.mesh.PacketCounters
import com.astramesh.routing.RoutingEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiagnosticsUiState(
    val peers: List<Peer> = emptyList(),
    val relayQueueSize: Int = 0,
    val dedupCacheSize: Int = 0,
    val counters: PacketCounterSnapshot = PacketCounterSnapshot(),
) {
    val directPeers: List<Peer> get() = peers.filter { it.isConnected }
    val relayCapablePeers: List<Peer> get() = peers.filter { it.isConnected && it.node.supportsRelay }
}

/**
 * Backs the developer diagnostics screen (docs milestone: Settings → Diagnostics): discovered
 * peers, session state, relay queue size, packet counters, dedup cache size, and route table.
 *
 * [RoutingEngine.dedupCacheSize] is a plain (non-reactive) property, so it's sampled on a short
 * poll instead of exposed as a Flow from the pure-Kotlin routing module — keeping that module
 * free of any UI-refresh concerns.
 */
@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    peers: PeerRepository,
    relayQueue: RelayQueueRepository,
    private val routing: RoutingEngine,
    counters: PacketCounters,
) : ViewModel() {

    private val dedupCacheSize = MutableStateFlow(routing.dedupCacheSize)

    val uiState: StateFlow<DiagnosticsUiState> =
        combine(
            peers.observePeers(),
            relayQueue.observeQueueSize(),
            dedupCacheSize,
            counters.snapshot,
        ) { peerList, queueSize, dedupSize, counterSnapshot ->
            DiagnosticsUiState(
                peers = peerList,
                relayQueueSize = queueSize,
                dedupCacheSize = dedupSize,
                counters = counterSnapshot,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DiagnosticsUiState(),
        )

    init {
        viewModelScope.launch {
            while (true) {
                delay(2_000)
                dedupCacheSize.value = routing.dedupCacheSize
            }
        }
    }
}
