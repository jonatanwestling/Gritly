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

// represents the different screens in the app with their respective routes, labels, and icons.
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Today : Screen("today", "Gritly", Icons.Default.Home)
    object AddGoal : Screen("add_goal", "Plan", Icons.Default.DateRange)
    object History : Screen("history", "History", Icons.Default.Refresh)
}

/**
 * MainScreen is the main entry point of the Gritly app.
 * It sets up the navigation structure and displays different screens based on user interaction.
 *
 * @param taskViewModel The ViewModel that provides the task data and handles task operations.
 * @param pictureViewModel The ViewModel that handles the daily entries with pictures and comments.
 */
@Composable
fun MainScreen(taskViewModel: TaskViewModel, pictureViewModel: PictureViewModel) {
    val navController = rememberNavController()

    val items = listOf(Screen.Today, Screen.AddGoal, Screen.History)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val currentScreen = items.find { it.route == currentRoute } ?: Screen.Today

    Scaffold(topBar = {
        // simple top bar to display the current title of the screen
        GritlyTopBar(title = currentScreen.label)
    }, bottomBar = {
        // bottom navigation bar that allows users to switch between different screens
        NavigationBar {
            items.forEach { screen ->
                NavigationBarItem(
                    icon = { Icon(screen.icon, contentDescription = screen.label) },
                    label = { Text(screen.label) },
                    selected = currentRoute == screen.route,
                    onClick = {
                        // navigate to the selected screen, not the current
                        if (currentRoute != screen.route) {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    // pop up to the start destination to avoid building up the
                                    // back stack
                                    saveState = true
                                }
                                //save and restore state like scroll positions
                                restoreState = true
                            }
                        }
                    })
            }
        }
    }) { innerPadding ->
        // navigation host that contains the different screens of the app
        NavHost(
            navController = navController,
            startDestination = Screen.Today.route,
            Modifier.padding(innerPadding)
        ) {
            // composable functions for each screen provided with necessary ViewModels
            composable(Screen.Today.route) { TodayScreen(taskViewModel, pictureViewModel) }
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
    CenterAlignedTopAppBar(title = { Text(title) }, navigationIcon = {}, actions = {})
}