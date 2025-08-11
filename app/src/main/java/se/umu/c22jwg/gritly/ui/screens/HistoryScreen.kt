package se.umu.c22jwg.gritly.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import se.umu.c22jwg.gritly.ui.viewmodel.PictureViewModel
import se.umu.c22jwg.gritly.ui.viewmodel.TaskViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

@Composable
fun HistoryScreen(taskViewModel: TaskViewModel, pictureViewModel: PictureViewModel) {
    val entries by pictureViewModel.allEntries
    val bitmap by pictureViewModel.bitmap
    val allTasks by taskViewModel.allTasks.collectAsState()

    LaunchedEffect(Unit) {
        pictureViewModel.loadAllEntries()
    }

    // Group tasks by date (no filtering)
    val tasksByDate = remember(allTasks) {
        allTasks.groupBy { it.date }
    }

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
                modifier = Modifier.fillMaxWidth(),
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
                        Image(
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