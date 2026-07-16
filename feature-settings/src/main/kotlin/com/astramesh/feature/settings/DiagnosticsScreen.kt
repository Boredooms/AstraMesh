package com.astramesh.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astramesh.domain.model.Peer
import com.astramesh.mesh.PacketCounterSnapshot
import com.astramesh.ui.theme.AstraSpacing
import com.astramesh.ui.theme.AstraSuccess
import com.astramesh.ui.theme.AstraTextDisabled

/**
 * Developer diagnostics (Settings → Diagnostics, hidden from the main nav — docs milestone
 * Phase D). Surfaces internal mesh state useful for debugging and demoing: discovered peers,
 * session state, relay queue size, packet counters, dedup cache size, and the route table
 * (direct vs. relay-capable peers, which is what the routing engine actually uses).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    onBack: () -> Unit,
    viewModel: DiagnosticsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diagnostics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(AstraSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AstraSpacing.md),
        ) {
            item { SectionCard(title = "Mesh Counters") { CounterGrid(state.counters) } }
            item {
                SectionCard(title = "Routing State") {
                    Column {
                        MetricRow("Dedup cache size", state.dedupCacheSize.toString())
                        MetricRow("Relay queue size (this node)", state.relayQueueSize.toString())
                        MetricRow("Known peers", state.peers.size.toString())
                        MetricRow("Direct peers", state.directPeers.size.toString())
                        MetricRow("Relay-capable peers", state.relayCapablePeers.size.toString())
                    }
                }
            }
            item { Text("Route Table", style = MaterialTheme.typography.titleSmall) }
            items(state.peers, key = { it.nodeId }) { peer -> RouteTableRow(peer) }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(AstraSpacing.lg)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            HorizontalDivider(modifier = Modifier.padding(vertical = AstraSpacing.sm))
            content()
        }
    }
}

@Composable
private fun CounterGrid(counters: PacketCounterSnapshot) {
    Column {
        MetricRow("Chat sent", counters.chatSent.toString())
        MetricRow("Chat received", counters.chatReceived.toString())
        MetricRow("Relayed", counters.relayed.toString())
        MetricRow("ACKs sent", counters.acksSent.toString())
        MetricRow("ACKs received", counters.acksReceived.toString())
        MetricRow("Handshakes sent", counters.handshakesSent.toString())
        MetricRow("Handshakes received", counters.handshakesReceived.toString())
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AstraSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun RouteTableRow(peer: Peer) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(AstraSpacing.md)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(peer.nodeId, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
                Text(
                    text = peer.sessionState.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (peer.isConnected) AstraSuccess else AstraTextDisabled,
                )
            }
            Text(
                text = "relay=${peer.node.supportsRelay} signal=${peer.signalStrength ?: "n/a"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
