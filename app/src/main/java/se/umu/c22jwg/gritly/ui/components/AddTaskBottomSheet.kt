package se.umu.c22jwg.gritly.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = {
                        if (taskName.isNotBlank()) {
                            taskViewModel.addTask(
                                title = taskName,
                                date = selectedDate
                            )
                            coroutineScope.launch {
                                sheetState.hide()
                            }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    onDismiss()
                                    taskName = ""
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