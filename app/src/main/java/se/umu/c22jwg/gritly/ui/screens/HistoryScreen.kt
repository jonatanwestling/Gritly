package se.umu.c22jwg.gritly.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import se.umu.c22jwg.gritly.data.Task
import se.umu.c22jwg.gritly.ui.viewmodel.PictureViewModel
import se.umu.c22jwg.gritly.ui.viewmodel.TaskViewModel
import se.umu.c22jwg.gritly.utils.cleanDate
import java.io.File
import androidx.compose.runtime.livedata.observeAsState

/**
 * HistoryScreen is a composable function that displays a feed of past daily entries with their
 * associated tasks.It allows users to view completed tasks for each date and shows a bottom sheet
 * with detailed task information when a card is clicked.
 *
 * @param taskViewModel The ViewModel that provides the task data and handles task operations.
 * @param pictureViewModel The ViewModel that handles the daily entries and pictures.
 */
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HistoryScreen(taskViewModel: TaskViewModel, pictureViewModel: PictureViewModel) {
    val entries by pictureViewModel.allEntries
    val bitmap by pictureViewModel.bitmap
    val allTasks by taskViewModel.allTasks.observeAsState(emptyList())
    // state variables for the bottom sheet
    var selectedDateForSheet by remember { mutableStateOf<String?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    // load all daily entries when the screen is launched
    LaunchedEffect(Unit) {
        pictureViewModel.loadAllEntries()
    }

    // group tasks by date
    val tasksByDate = remember(allTasks) {
        allTasks.groupBy { it.date }
    }


    Scaffold {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(entries) { entry ->

                val tasksForThisDate = tasksByDate[entry.date] ?: emptyList()
                val totalTasks = tasksForThisDate.size
                val completedTasks = tasksForThisDate.count { it.isCompleted }
                // calculate the percentage of completed tasks
                val percentCompleted =
                    if (totalTasks > 0) (completedTasks * 100 / totalTasks) else 0

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedDateForSheet = entry.date
                            showBottomSheet = true
                        },
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 16.dp)
                        ) {
                            Text(
                                text = cleanDate(entry.date),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Completed: $percentCompleted% ($completedTasks / $totalTasks)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            // show note just if it is not empty
                            if (entry.note.isNotBlank()) {
                                Text(
                                    text = entry.note,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                        // show the image if the bitmap is not null
                        if (entry.imagePath != null)
                        AsyncImage(
                            model = File(entry.imagePath),
                            contentDescription = "Daily photo",
                            modifier = Modifier
                                .size(134.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
        if (showBottomSheet) {
            TaskBottomSheet(
                onDismiss = { showBottomSheet = false },
                tasks = tasksByDate[selectedDateForSheet] ?: emptyList(),
                title = "Tasks for ${cleanDate(selectedDateForSheet!!)}"
            )
        }
    }
}

/**
 * TaskBottomSheet is a composable function that displays a bottom sheet with a list of tasks for a specific date.
 * It shows the tasks in a LazyColumn displayed like  in the today screen but greyed out to indicate that the past cant
 * be changed.
 *
 * @param onDismiss Callback to dismiss the bottom sheet.
 * @param tasks List of tasks to display in the bottom sheet.
 * @param title Title of the bottom sheet.
 * @param modifier Modifier for additional styling or layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskBottomSheet(
    onDismiss: () -> Unit, tasks: List<Task>, title: String, modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = sheetState
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            // displays the tasks in a scrollable list as in the TodayScreen
            LazyColumn(
                modifier = Modifier.fillMaxHeight(0.6f)
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskRow(
                        task = task, modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * TaskRow is a composable function that displays a single task in a row format.
 * This code is a slightly modified version of the TaskRow from the TodayScreen.
 * It is used in the TaskBottomSheet to show past tasks and does not allow any
 * interaction with the task.
 *
 * @param task The task to display.
 * @param modifier Modifier for additional styling or layout.
 */
@Composable
fun TaskRow(task: Task, modifier: Modifier = Modifier) {
    // Make the text grey so user understand it is disabled
    val textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    Row(
        modifier = modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.isCompleted,
            // Disable the the interaction with the checkbox
            onCheckedChange = null, enabled = false
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = task.title, style = MaterialTheme.typography.bodyLarge.copy(
                // Display the the text as normal
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                color = textColor
            )
        )
    }
}
