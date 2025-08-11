package se.umu.c22jwg.gritly.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import se.umu.c22jwg.gritly.data.Task
import se.umu.c22jwg.gritly.ui.viewmodel.PictureViewModel
import se.umu.c22jwg.gritly.ui.viewmodel.TaskViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HistoryScreen(taskViewModel: TaskViewModel, pictureViewModel: PictureViewModel) {
    val entries by pictureViewModel.allEntries
    val bitmap by pictureViewModel.bitmap
    val allTasks by taskViewModel.allTasks.collectAsState()

    var selectedDateForSheet by remember { mutableStateOf<String?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        pictureViewModel.loadAllEntries()
    }

    // Group tasks by date (no filtering)
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
                val percentCompleted = if (totalTasks > 0) (completedTasks * 100 / totalTasks) else 0

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
                                text = formatHumanDate(entry.date),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Completed: $percentCompleted% ($completedTasks / $totalTasks)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            if (entry.note.isNotBlank()) {
                                Text(
                                    text = entry.note,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                        bitmap?.let {
                            androidx.compose.foundation.Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "Captured Image",
                                modifier = Modifier
                                    .size(134.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }

        if (showBottomSheet && selectedDateForSheet != null) {
            TaskBottomSheet(
                showBottomSheet = showBottomSheet,
                onDismiss = { showBottomSheet = false },
                tasks = tasksByDate[selectedDateForSheet] ?: emptyList(),
                title = "Tasks for ${formatHumanDate(selectedDateForSheet!!)}"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskBottomSheet(
    showBottomSheet: Boolean,
    onDismiss: () -> Unit,
    tasks: List<Task>,
    title: String,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState()

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState
        ) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxHeight(0.6f) // Limit height a bit
                ) {
                    items(tasks, key = { it.id }) { task ->
                        TaskRow(
                            task = task,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TaskRow(task: Task, modifier: Modifier = Modifier) {
    // Make the text grey so user understand it is disabled
    val textColor =  MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    Row(
        modifier = modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = null,
            enabled = false
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyLarge.copy(
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                color = textColor
            )
        )
    }
}

fun formatHumanDate(dateStr: String): String {
    val formatter = DateTimeFormatter.ISO_LOCAL_DATE
    val date = LocalDate.parse(dateStr, formatter)
    val today = LocalDate.now()
    return when (ChronoUnit.DAYS.between(date, today)) {
        0L -> "Today"
        1L -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault()))
    }
}