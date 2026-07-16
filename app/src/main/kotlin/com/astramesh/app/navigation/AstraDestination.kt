package com.astramesh.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Top-level navigation destinations. Each maps to a feature module screen.
 */
enum class AstraDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Discovery("discovery", "Nearby", Icons.Filled.Wifi),
    Chat("chat", "Chat", Icons.AutoMirrored.Filled.Chat),
    Files("files", "Files", Icons.Filled.Folder),
    Broadcast("broadcast", "Alert", Icons.Filled.Campaign),
    Settings("settings", "Settings", Icons.Filled.Settings);

    companion object {
        val START = Discovery
    }
}
