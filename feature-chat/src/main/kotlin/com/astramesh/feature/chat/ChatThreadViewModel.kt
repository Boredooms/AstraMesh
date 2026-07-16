package com.astramesh.feature.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astramesh.domain.model.Message
import com.astramesh.domain.repository.MessageRepository
import com.astramesh.domain.repository.PeerRepository
import com.astramesh.mesh.MeshCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatThreadUiState(
    val peerId: String = "",
    val peerName: String = "",
    val peerReachable: Boolean = false,
    val messages: List<Message> = emptyList(),
    val draft: String = "",
)

/**
 * Drives a single conversation thread (docs/design.md §8 "chat thread screen").
 * Sending persists the message first, then routes/encrypts/sends it via [MeshCoordinator]
 * (docs/workflow.md §7) — the UI never touches transport or crypto directly.
 */
@HiltViewModel
class ChatThreadViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val messages: MessageRepository,
    private val peers: PeerRepository,
    private val coordinator: MeshCoordinator,
) : ViewModel() {

    private val peerId: String = checkNotNull(savedStateHandle["peerId"]) { "peerId is required" }
    private val draft = MutableStateFlow("")

    val uiState: StateFlow<ChatThreadUiState> =
        combine(
            messages.observeConversation(peerId),
            peers.observePeers(),
            draft,
        ) { conversation, peerList, draftText ->
            val peer = peerList.firstOrNull { it.nodeId == peerId }
            ChatThreadUiState(
                peerId = peerId,
                peerName = peer?.node?.deviceName ?: peerId,
                peerReachable = peer?.isConnected == true,
                messages = conversation.sortedBy { it.timestamp },
                draft = draftText,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ChatThreadUiState(peerId = peerId),
        )

    fun onDraftChange(text: String) {
        draft.value = text
    }

    fun send() {
        val text = draft.value.trim()
        if (text.isEmpty()) return
        draft.value = ""
        viewModelScope.launch {
            coordinator.sendChat(peerId, text)
        }
    }
}
