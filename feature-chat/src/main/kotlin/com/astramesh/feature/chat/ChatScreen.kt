package com.astramesh.feature.chat

import androidx.compose.runtime.Composable
import com.astramesh.feature.chat.ui.FeatureScaffold

/**
 * Chat screen entry point.
 *
 * Stage 0 renders a titled scaffold so the navigation shell is fully wired and the
 * app launches end to end. Real feature logic is added in later stages.
 */
@Composable
fun ChatScreen() {
    FeatureScaffold(
        title = "Chat",
        subtitle = "Encrypted, relayed, store-and-forward messaging.",
    )
}
