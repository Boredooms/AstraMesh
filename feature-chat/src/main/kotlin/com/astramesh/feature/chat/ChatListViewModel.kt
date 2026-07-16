package com.astramesh.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astramesh.domain.model.Message
import com.astramesh.domain.model.Node
import com.astramesh.domain.repository.MessageRepository
import com.astramesh.domain.repository.PeerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** One row in the conversation list: the counterpart node plus their most recent message. */
data class ConversationSummary(
    val peerId: String,
    val displayName: String,
    val lastMessage: Message,
)

data class ChatListUiState(
    val conversations: List<ConversationSummary> = emptyList(),
)

/**
 * Drives the conversation list (docs/design.md §8 "chat list screen"). One row per peer with
 * a conversation, most recent activity first — matches [MessageRepository.observeConversations].
 */
@HiltViewModel
class ChatListViewModel @Inject constructor(
    messages: MessageRepository,
    peers: PeerRepository,
) : ViewModel() {

    val uiState: StateFlow<ChatListUiState> =
        combine(messages.observeConversations(), peers.observePeers()) { lastPerPeer, peerList ->
            val names: Map<String, Node> = peerList.associate { it.nodeId to it.node }
            val summaries = lastPerPeer
                .map { msg ->
                    val peerId = if (msg.outgoing) msg.receiverId else msg.senderId
                    ConversationSummary(
                        peerId = peerId,
                        displayName = names[peerId]?.deviceName ?: peerId,
                        lastMessage = msg,
                    )
                }
                .sortedByDescending { it.lastMessage.timestamp }
            ChatListUiState(conversations = summaries)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ChatListUiState(),
        )
}
