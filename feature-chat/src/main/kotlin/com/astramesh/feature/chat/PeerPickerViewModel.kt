package com.astramesh.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astramesh.domain.model.Peer
import com.astramesh.domain.repository.PeerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class PeerPickerUiState(
    val peers: List<Peer> = emptyList(),
)

/**
 * Drives the peer picker (docs milestone: "peer picker" — Chat UI, Phase A). Lists every
 * known peer, connected or not: picking an offline one is exactly what should trigger a new
 * handshake (or queue the first message for store-and-forward) once the thread opens.
 */
@HiltViewModel
class PeerPickerViewModel @Inject constructor(
    peers: PeerRepository,
) : ViewModel() {

    val uiState: StateFlow<PeerPickerUiState> =
        peers.observePeers()
            .map { list -> PeerPickerUiState(peers = list.sortedByDescending { it.lastContact }) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = PeerPickerUiState(),
            )
}
