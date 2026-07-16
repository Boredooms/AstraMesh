package com.astramesh.feature.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astramesh.domain.model.Peer
import com.astramesh.ui.theme.AstraSpacing
import com.astramesh.ui.theme.AstraSuccess
import com.astramesh.ui.theme.AstraTextDisabled

/**
 * Peer picker: choose a known node to start (or resume) a conversation with (docs milestone
 * "peer picker" — Chat UI, Phase A). Selecting a peer opens the thread, which itself triggers
 * the secure handshake if there isn't an active session yet (see [ChatThreadViewModel]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeerPickerScreen(
    onBack: () -> Unit,
    onPeerSelected: (peerId: String) -> Unit,
    viewModel: PeerPickerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Chat") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (state.peers.isEmpty()) {
            EmptyPeerList(modifier = Modifier.fillMaxSize().padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(AstraSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(AstraSpacing.sm),
            ) {
                items(state.peers, key = { it.nodeId }) { peer ->
                    PickablePeerRow(peer = peer, onClick = { onPeerSelected(peer.nodeId) })
                }
            }
        }
    }
}

@Composable
private fun PickablePeerRow(peer: Peer, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AstraSpacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AstraSpacing.md),
        ) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape)) {
                Surface(
                    color = if (peer.isConnected) AstraSuccess else AstraTextDisabled,
                    modifier = Modifier.fillMaxSize(),
                ) {}
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(peer.node.deviceName, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = peer.nodeId,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = if (peer.isConnected) "Connected" else "Offline",
                style = MaterialTheme.typography.labelSmall,
                color = if (peer.isConnected) AstraSuccess else AstraTextDisabled,
            )
        }
    }
}

@Composable
private fun EmptyPeerList(modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(AstraSpacing.xxl), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.BluetoothSearching,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "No peers known yet",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = AstraSpacing.lg),
            )
            Text(
                text = "Discover nearby nodes from the Nearby tab first.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = AstraSpacing.xs),
            )
        }
    }
}
