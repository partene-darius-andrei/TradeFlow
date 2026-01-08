package com.dpart.tradeflow.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val icon: ImageVector? = null, val label: String? = null) {
    data object Dashboard : Screen(route = "dashboard", icon = Icons.Default.Dashboard, label = "Dashboard")
    data object Settings : Screen(route = "settings", icon = Icons.Default.Settings, label = "Settings")
}

val screensWithBottomNav = listOf(
    Screen.Dashboard.route,
    Screen.Settings.route
)
