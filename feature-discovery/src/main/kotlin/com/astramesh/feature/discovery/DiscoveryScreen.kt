package com.astramesh.feature.discovery

import androidx.compose.runtime.Composable
import com.astramesh.feature.discovery.ui.FeatureScaffold

/**
 * Nearby Nodes screen entry point.
 *
 * Stage 0 renders a titled scaffold so the navigation shell is fully wired and the
 * app launches end to end. Real feature logic is added in later stages.
 */
@Composable
fun DiscoveryScreen() {
    FeatureScaffold(
        title = "Nearby Nodes",
        subtitle = "Scanning for AstraMesh peers over Bluetooth LE.",
    )
}
