package com.astramesh.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.astramesh.feature.broadcast.BroadcastScreen
import com.astramesh.feature.chat.ChatScreen
import com.astramesh.feature.discovery.DiscoveryScreen
import com.astramesh.feature.files.FilesScreen
import com.astramesh.feature.settings.SettingsScreen

/**
 * Root navigation shell. Hosts the five top-level feature screens behind a bottom bar.
 */
@Composable
fun AstraMeshApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                AstraDestination.entries.forEach { dest ->
                    val selected =
                        currentDestination?.hierarchy?.any { it.route == dest.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AstraDestination.START.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(AstraDestination.Discovery.route) { DiscoveryScreen() }
            composable(AstraDestination.Chat.route) { ChatScreen() }
            composable(AstraDestination.Files.route) { FilesScreen() }
            composable(AstraDestination.Broadcast.route) { BroadcastScreen() }
            composable(AstraDestination.Settings.route) { SettingsScreen() }
        }
    }
}
