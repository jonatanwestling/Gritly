package se.umu.c22jwg.gritly.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import androidx.navigation.compose.NavHost
import se.umu.c22jwg.gritly.ui.viewmodel.TaskViewModel

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Today : Screen("today", "Today", Icons.Default.Home)
    object AddGoal : Screen("add_goal", "Plan", Icons.Default.DateRange)
    object History : Screen("history", "History", Icons.Default.Refresh)
}

@Composable
fun MainScreen(taskViewModel: TaskViewModel) {
    val navController = rememberNavController()
    val items = listOf(Screen.Today, Screen.AddGoal, Screen.History)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val currentScreen = items.find { it.route == currentRoute } ?: Screen.Today

    Scaffold(
        topBar = {
            GritlyTopBar(title = currentScreen.label)
        },
        bottomBar = {
            NavigationBar {
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
            ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Today.route,
            Modifier.padding(innerPadding)
        ) {
            composable(Screen.Today.route) { TodayScreen(taskViewModel) }
            composable(Screen.AddGoal.route) { AddGoalScreen(taskViewModel) }
            composable(Screen.History.route) { HistoryScreen(taskViewModel) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GritlyTopBar(title: String) {
    CenterAlignedTopAppBar(
        title = { Text(title) },
        navigationIcon = {},
        actions = {}
    )
}