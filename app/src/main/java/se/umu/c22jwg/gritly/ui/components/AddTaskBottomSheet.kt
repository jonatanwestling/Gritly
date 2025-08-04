package se.umu.c22jwg.gritly.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import se.umu.c22jwg.gritly.ui.viewmodel.TaskViewModel


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
    val sheetState = rememberModalBottomSheetState()
    var taskName by rememberSaveable { mutableStateOf("") }
    var isRecurring by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

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
                TextField(
                    value = taskName,
                    onValueChange = { taskName = it },
                    label = { Text("Task name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                
                // Recurring task switch
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
                            text = if (isRecurring) "Task will repeat every day" else "Task will only appear once",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = isRecurring,
                        onCheckedChange = { isRecurring = it }
                    )
                }
                
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = {
                        if (taskName.isNotBlank()) {
                            if (isRecurring) {
                                // Add recurring task for multiple days
                                taskViewModel.addRecurringTask(
                                    title = taskName,
                                    startDate = selectedDate
                                )
                            } else {
                                // Add single task
                                taskViewModel.addTask(
                                    title = taskName,
                                    date = selectedDate
                                )
                            }
                            coroutineScope.launch {
                                sheetState.hide()
                            }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    onDismiss()
                                    taskName = ""
                                    isRecurring = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = taskName.isNotBlank()
                ) {
                    Text("Add Task")
                }
            }
        }
    }
} 