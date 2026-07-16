package com.astramesh.feature.broadcast

import androidx.compose.runtime.Composable
import com.astramesh.feature.broadcast.ui.FeatureScaffold

/**
 * Emergency Broadcast screen entry point.
 *
 * Stage 0 renders a titled scaffold so the navigation shell is fully wired and the
 * app launches end to end. Real feature logic is added in later stages.
 */
@Composable
fun BroadcastScreen() {
    FeatureScaffold(
        title = "Emergency Broadcast",
        subtitle = "High-priority alerts that flood the mesh.",
    )
}
