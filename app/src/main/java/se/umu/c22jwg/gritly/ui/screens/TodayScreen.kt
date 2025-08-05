package se.umu.c22jwg.gritly.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import se.umu.c22jwg.gritly.ui.theme.GritlyTheme
import se.umu.c22jwg.gritly.ui.viewmodel.TaskViewModel
import se.umu.c22jwg.gritly.data.Task
import se.umu.c22jwg.gritly.ui.components.AddTaskBottomSheet
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    taskViewModel: TaskViewModel,
    modifier: Modifier = Modifier,
    ) {

    val allTasks by taskViewModel.allTasks.collectAsState()
    val today = LocalDate.now().toString()
    val todayTasks = allTasks.filter { it.date == today }

    // Bottom sheet state
    var showBottomSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = { showBottomSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { paddingValues ->
        Column (
            modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
        ){
            ProgressBar(todayTasks)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                items(todayTasks, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        modifier = Modifier.animateItem(),
                        onToggle = {
                            taskViewModel.toggleTaskCompletion(task)
                        }
                    )
                }
            }
        }
        
        // AddTaskBottomSheet component
        AddTaskBottomSheet(
            showBottomSheet = showBottomSheet,
            onDismiss = { showBottomSheet = false },
            taskViewModel = taskViewModel,
            title = "Add New Task For Today",
            selectedDate = today
        )
    }
}

/**
 * Function that renders the TaskRow inside the list. It is a row of text to the left and
 * a checkbox to the right. The whole row is clickable.
 *
 * @param task The Task object representing the data for this row.
 * @param modifier Modifier to be applied to the row container.
 * @param onToggle Lambda invoked when either the checkbox or row is clicked to toggle completion.
 */
@Composable
fun TaskRow(
    task: Task,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onToggle() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = task.title,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            style = if (task.isCompleted) {
                LocalTextStyle.current.copy(textDecoration = TextDecoration.LineThrough)
            } else {
                LocalTextStyle.current
            }
        )
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = { onToggle() }
        )
    }
}



@Composable
fun ProgressBar(tasks: List<Task>) {
    val completed = tasks.count { it.isCompleted }
    val total = tasks.size
    val progress = if (total > 0) completed / total.toFloat() else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(text = "$completed/$total tasks completed")
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = ProgressIndicatorDefaults.linearColor,
                trackColor = ProgressIndicatorDefaults.linearTrackColor,

            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GritlyTheme {
        Text("TodayScreen Preview - Requires TaskViewModel")
    }
}