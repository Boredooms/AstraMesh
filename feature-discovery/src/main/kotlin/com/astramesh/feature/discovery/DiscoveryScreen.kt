package com.astramesh.feature.discovery

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astramesh.domain.model.Peer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(
    viewModel: DiscoveryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Manifest permissions alone don't grant BLUETOOTH_SCAN/ADVERTISE/CONNECT on API 31+ —
    // they must be requested at runtime, or startAdvertising()/startScan() fail silently with
    // a SecurityException the transport layer swallows (this is why two nearby devices could
    // never see each other: neither side's radio ever actually turned on).
    var hasPermission by remember { mutableStateOf(BlePermissions.allGranted(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        hasPermission = grants.values.all { it }
        if (hasPermission) viewModel.startScan()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Nearby Nodes") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            ScanControls(
                scanning = state.scanning,
                available = state.transportAvailable,
                peerCount = state.peers.size,
                onStart = {
                    if (hasPermission) {
                        viewModel.startScan()
                    } else {
                        permissionLauncher.launch(BlePermissions.required())
                    }
                },
                onStop = viewModel::stopScan,
            )

            state.lastError?.let { error ->
                Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            if (!hasPermission) {
                PermissionNeeded(
                    modifier = Modifier.fillMaxSize(),
                    onRequest = { permissionLauncher.launch(BlePermissions.required()) },
                )
            } else if (state.peers.isEmpty()) {
                EmptyPeers(scanning = state.scanning, modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.peers, key = { it.nodeId }) { peer -> PeerCard(peer) }
                }
            }
        }
    }
}

@Composable
private fun ScanControls(
    scanning: Boolean,
    available: Boolean,
    peerCount: Int,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = if (scanning) "Scanning" else "Idle",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = if (!available) "Bluetooth unavailable"
                    else "$peerCount peer${if (peerCount == 1) "" else "s"} nearby",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (scanning) {
                OutlinedButton(onClick = onStop) { Text("Stop") }
            } else {
                Button(onClick = onStart, enabled = available) { Text("Scan") }
            }
        }
    }
}

@Composable
private fun PeerCard(peer: Peer) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SignalDot(peer.signalStrength)
            Column(modifier = Modifier.weight(1f)) {
                Text(peer.node.deviceName, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = peer.nodeId,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SessionChip(peer.sessionState.name)
        }
    }
}

@Composable
private fun SignalDot(strength: Int?) {
    val color = when {
        strength == null -> MaterialTheme.colorScheme.onSurfaceVariant
        strength > -60 -> MaterialTheme.colorScheme.primary
        strength > -80 -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(modifier = Modifier.size(12.dp).clip(CircleShape)) {
        Surface(color = color, modifier = Modifier.fillMaxSize()) {}
    }
}

@Composable
private fun SessionChip(label: String) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.small) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun PermissionNeeded(onRequest: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.BluetoothSearching,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Bluetooth permission needed",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = "AstraMesh needs Bluetooth permission to discover and connect to " +
                    "nearby nodes. Nothing is shared until you grant it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
            Button(onClick = onRequest, modifier = Modifier.padding(top = 16.dp)) {
                Text("Grant permission")
            }
        }
    }
}

@Composable
private fun EmptyPeers(scanning: Boolean, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.BluetoothSearching,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (scanning) "Listening for nearby nodes…" else "No nodes yet",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = if (scanning)
                    "Keep the app open near another AstraMesh device."
                else
                    "Tap Scan to discover peers over Bluetooth LE.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
