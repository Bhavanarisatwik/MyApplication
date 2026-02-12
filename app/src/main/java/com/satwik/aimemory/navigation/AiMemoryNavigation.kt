package com.satwik.aimemory.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.satwik.aimemory.ui.screens.ask.AskRetrieveScreen
import com.satwik.aimemory.ui.screens.listening.ListeningScreen
import com.satwik.aimemory.ui.screens.settings.SettingsScreen
import com.satwik.aimemory.ui.screens.timeline.MemoryTimelineScreen
import com.satwik.aimemory.ui.theme.*

@Composable
fun AiMemoryNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        containerColor = DarkBackground,
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                contentColor = TextPrimary,
                tonalElevation = 0.dp
            ) {
                Screen.items.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any {
                        it.route == screen.route
                    } == true

                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentTeal,
                            selectedTextColor = AccentTeal,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = AccentTeal.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Listening.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(300))
            }
        ) {
            composable(Screen.Listening.route) {
                ListeningScreen()
            }
            composable(Screen.Timeline.route) {
                MemoryTimelineScreen()
            }
            composable(Screen.Ask.route) {
                AskRetrieveScreen(
                    onNavigateToTimeline = { summaryId ->
                        // Navigate to timeline (deep link will be enhanced in Phase 5)
                        navController.navigate(Screen.Timeline.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
