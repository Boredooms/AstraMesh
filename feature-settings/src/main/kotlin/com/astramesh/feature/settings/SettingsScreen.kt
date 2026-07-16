package com.astramesh.feature.settings

import androidx.compose.runtime.Composable
import com.astramesh.feature.settings.ui.FeatureScaffold

/**
 * Settings screen entry point.
 *
 * Stage 0 renders a titled scaffold so the navigation shell is fully wired and the
 * app launches end to end. Real feature logic is added in later stages.
 */
@Composable
fun SettingsScreen() {
    FeatureScaffold(
        title = "Settings",
        subtitle = "Node identity, keys, and diagnostics.",
    )
}
