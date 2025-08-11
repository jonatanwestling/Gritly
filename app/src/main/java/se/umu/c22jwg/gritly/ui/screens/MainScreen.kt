package se.umu.c22jwg.gritly.ui.screens

import android.graphics.Bitmap
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
import se.umu.c22jwg.gritly.ui.viewmodel.PictureViewModel
import se.umu.c22jwg.gritly.ui.viewmodel.TaskViewModel

/**
 * MainScreen is the primary navigation container for the Gritly app
 * It provides a bottom navigation bar with three screens:
 * - Today: Displays today's tasks
 * - AddGoal: Allows users to add new tasks
 * - History: Shows the history of completed tasks
 * 
 * @param taskViewModel The ViewModel that provides the task data and handles task operations.
 */

 // Represents the different screens in the app with their respective routes, labels, and icons.
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Today : Screen("today", "Gritly", Icons.Default.Home)
    object AddGoal : Screen("add_goal", "Plan", Icons.Default.DateRange)
    object History : Screen("history", "History", Icons.Default.Refresh)
}

@Composable
fun MainScreen(taskViewModel: TaskViewModel, pictureViewModel: PictureViewModel) {
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
            // The NavigationBar is the bottom navigation bar that allows users to switch between different screens.
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
                // The NavHost is the main content area that displays the current screen based on the navigation state.
        NavHost(
            navController = navController,
            startDestination = Screen.Today.route,
            Modifier.padding(innerPadding)
        ) {
            composable(Screen.Today.route) { TodayScreen(taskViewModel, pictureViewModel ) }
            composable(Screen.AddGoal.route) { AddGoalScreen(taskViewModel) }
            composable(Screen.History.route) { HistoryScreen(taskViewModel, pictureViewModel) }
        }
    }
}

/**
 * GritlyTopBar is a top app bar that displays the title of the current screen.
 * It is used in the MainScreen to provide a consistent header across different screens.
 * 
 * @param title The title to be displayed in the top bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GritlyTopBar(title: String) {
    CenterAlignedTopAppBar(
        title = { Text(title) },
        navigationIcon = {},
        actions = {}
    )
}