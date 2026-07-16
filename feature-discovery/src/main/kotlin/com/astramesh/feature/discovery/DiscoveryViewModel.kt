package com.astramesh.feature.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astramesh.domain.identity.NodeIdentityProvider
import com.astramesh.domain.model.Capability
import com.astramesh.domain.model.Node
import com.astramesh.domain.model.Peer
import com.astramesh.domain.model.PlatformType
import com.astramesh.domain.model.SessionState
import com.astramesh.domain.repository.PeerRepository
import com.astramesh.transport.Transport
import com.astramesh.transport.TransportEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiscoveryUiState(
    val scanning: Boolean = false,
    val transportAvailable: Boolean = true,
    val peers: List<Peer> = emptyList(),
    val lastError: String? = null,
)

/**
 * Drives peer discovery (docs/workflow.md §5). Starts the [Transport], turns discovery events
 * into persisted [Peer] records, and exposes them as reactive UI state. The UI never touches
 * Bluetooth APIs directly (docs/architecture.md §3).
 */
@HiltViewModel
class DiscoveryViewModel @Inject constructor(
    private val transport: Transport,
    private val peerRepository: PeerRepository,
    private val identity: NodeIdentityProvider,
) : ViewModel() {

    private val scanning = MutableStateFlow(false)
    private val lastError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DiscoveryUiState> =
        combine(
            peerRepository.observePeers(),
            scanning,
            lastError,
        ) { peers, isScanning, error ->
            DiscoveryUiState(
                scanning = isScanning,
                transportAvailable = transport.isAvailable(),
                peers = peers.sortedByDescending { it.lastContact },
                lastError = error,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DiscoveryUiState(),
        )

    init {
        collectTransportEvents()
    }

    fun startScan() {
        if (scanning.value) return
        scanning.value = true
        viewModelScope.launch { transport.start(identity.nodeId()) }
    }

    fun stopScan() {
        scanning.value = false
        viewModelScope.launch { transport.stop() }
    }

    private fun collectTransportEvents() {
        transport.events
            .onEach { event ->
                when (event) {
                    is TransportEvent.PeerDiscovered -> upsertDiscovered(event)
                    is TransportEvent.PeerLost -> peerRepository.removePeer(event.nodeId)
                    is TransportEvent.Error -> lastError.value = event.message
                    else -> Unit
                }
            }
            .launchIn(viewModelScope)
    }

    private suspend fun upsertDiscovered(event: TransportEvent.PeerDiscovered) {
        val e = event.endpoint
        val now = System.currentTimeMillis()
        val node = peerRepository.getNode(e.nodeId)?.copy(lastSeen = now) ?: Node(
            nodeId = e.nodeId,
            deviceName = e.nodeId.take(8),
            platformType = PlatformType.ANDROID,
            publicKey = "",
            keyFingerprint = "",
            capabilities = setOf(Capability.CHAT, Capability.RELAY),
            lastSeen = now,
            relayCapable = e.relaySupported,
        )
        peerRepository.upsertPeer(
            Peer(
                node = node,
                sessionState = SessionState.DISCOVERED,
                signalStrength = e.signalStrength,
                lastContact = now,
            )
        )
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch { transport.stop() }
    }
}
