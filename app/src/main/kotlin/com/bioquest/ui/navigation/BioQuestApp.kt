package com.bioquest.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bioquest.ui.BioQuestViewModel
import com.bioquest.ui.screens.DailyLogScreen
import com.bioquest.ui.screens.DashboardScreen
import com.bioquest.ui.screens.HistoryScreen
import com.bioquest.ui.screens.QuestBoardScreen
import com.bioquest.ui.screens.SettingsScreen

enum class Destination(val route: String, val label: String, val icon: ImageVector) {
    DASHBOARD("dashboard", "CORE", Icons.Filled.Dashboard),
    DAILY_LOG("log", "LOG", Icons.Filled.Edit),
    QUESTS("quests", "QUESTS", Icons.Filled.Assignment),
    HISTORY("history", "HISTORY", Icons.Filled.History),
    SETTINGS("settings", "CONFIG", Icons.Filled.Settings),
}

@Composable
fun BioQuestApp(viewModel: BioQuestViewModel) {
    val navController = rememberNavController()
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Every log action confirms itself with a short terminal-style snackbar.
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.feedback.collect { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val current = backStackEntry?.destination
                Destination.entries.forEach { dest ->
                    NavigationBarItem(
                        selected = current?.hierarchy?.any { it.route == dest.route } == true,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
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
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.DASHBOARD.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Destination.DASHBOARD.route) { DashboardScreen(state, viewModel) }
            composable(Destination.DAILY_LOG.route) { DailyLogScreen(state, viewModel) }
            composable(Destination.QUESTS.route) { QuestBoardScreen(state) }
            composable(Destination.HISTORY.route) { HistoryScreen(state, viewModel) }
            composable(Destination.SETTINGS.route) { SettingsScreen(state, viewModel) }
        }
    }
}
