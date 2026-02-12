package com.satwik.aimemory.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navigation destinations for the bottom navigation bar.
 */
sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Listening : Screen("listening", "Listen", Icons.Default.Hearing)
    data object Timeline : Screen("timeline", "Memory", Icons.Default.Timeline)
    data object Ask : Screen("ask", "Ask", Icons.Default.Psychology)
    data object Settings : Screen("settings", "Privacy", Icons.Default.Shield)

    companion object {
        val items = listOf(Listening, Timeline, Ask, Settings)
    }
}
