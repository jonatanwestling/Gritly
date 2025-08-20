package se.umu.c22jwg.gritly.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import se.umu.c22jwg.gritly.ui.viewmodel.TaskViewModel

/**
 * AddTaskBottomSheet is a composable function that displays a bottom sheet for adding new tasks.
 * It allows users to enter a task name, choose whether the task is recurring, and select the days
 * on which the task should repeat. The bottom sheet can be dismissed and will call the provided
 * onDismiss callback when closed.
 *
 * @param showBottomSheet Boolean to control the visibility of the bottom sheet.
 * @param onDismiss Callback function to be called when the bottom sheet is dismissed.
 * @param taskViewModel ViewModel for managing tasks.
 * @param selectedDate The date for which the task is being added.
 * @param title Title to display at the top of the bottom sheet.
 * @param modifier Modifier for styling and layout purposes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskBottomSheet(
    showBottomSheet: Boolean,
    onDismiss: () -> Unit,
    taskViewModel: TaskViewModel,
    selectedDate: String,
    title: String,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val verticalSpacing = if (isLandscape) 8.dp else 16.dp
    val bottomSpacing = if (isLandscape) 16.dp else 32.dp

    // limit bottom sheet width in landscape
    val sheetModifier = if (isLandscape) {
        Modifier
            .fillMaxWidth(0.6f)
    } else {
        Modifier
            .fillMaxWidth()
    }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    var taskName by rememberSaveable { mutableStateOf("") }
    var isRecurring by rememberSaveable { mutableStateOf(false) }
    // default all days selected
    var weekDaysSelected by rememberSaveable { mutableStateOf(List(7) { true }) }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            modifier = sheetModifier
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(verticalSpacing))
                TextField(
                    value = taskName,
                    onValueChange = { taskName = it },
                    label = { Text("Task name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(verticalSpacing))
                // recurring task switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Recurring Task",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isRecurring) "Task will repeat on selected days" else "Task will only appear once",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = isRecurring,
                        onCheckedChange = { isRecurring = it }
                    )
                }
                if (isRecurring) {
                    Spacer(Modifier.height(verticalSpacing))
                    Text("Repeat on", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                        for (i in 0..6) {
                            val selected = weekDaysSelected[i]
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    weekDaysSelected = weekDaysSelected.toMutableList().also { it[i] = !it[i] }
                                },
                                label = { Text(dayLabels[i]) },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(bottomSpacing))
                Button(
                    onClick = {
                        if (taskName.isNotBlank()) {
                            if (isRecurring) {
                                taskViewModel.addRecurringTask(
                                    title = taskName,
                                    days = weekDaysSelected
                                )
                            } else {
                                taskViewModel.addTask(
                                    title = taskName,
                                    date = selectedDate
                                )
                            }
                            onDismiss()
                            taskName = ""
                            isRecurring = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = taskName.isNotBlank() && ((weekDaysSelected.contains(true) && isRecurring) || !isRecurring)
                ) {
                    Text("Add Task")
                }
            }
        }
    }
}