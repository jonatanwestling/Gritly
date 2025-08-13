package se.umu.c22jwg.gritly.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import se.umu.c22jwg.gritly.ui.viewmodel.TaskViewModel
import se.umu.c22jwg.gritly.data.Task
import se.umu.c22jwg.gritly.ui.components.AddTaskBottomSheet
import java.time.LocalDate
import kotlinx.coroutines.delay
import se.umu.c22jwg.gritly.ui.viewmodel.DailyViewModel
import java.time.LocalTime
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch

/**
 * Composable function that displays the Today screen, showing active tasks for the current day
 * and allowing users to add new tasks via the bottom sheet triggered by the FAB. The user can
 * see a progress bar indicating the completion status of today's tasks. As well as see the tasks
 * that are left and the tasks that are completed in a list with checkboxes to toggle completion.
 *
 * @param taskViewModel The ViewModel that provides the task data and handles task operations.
 * @param modifier Modifier to be applied to the screen container.
 */
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    taskViewModel: TaskViewModel,
    dailyViewModel: DailyViewModel,
    modifier: Modifier = Modifier,
) {

    val today = LocalDate.now().toString()
    val todayTasks by taskViewModel.todayTasks.observeAsState(emptyList())
    val listState = rememberLazyListState()
    var scrollToTop by remember { mutableStateOf(false) }

    // Bottom sheet state
    var showBottomSheet by remember { mutableStateOf(false) }
    var showImageOptionsSheet by remember { mutableStateOf(false) }

    //Check camera permission
    val context = LocalContext.current
    val cameraPermission = Manifest.permission.CAMERA
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            dailyViewModel.takePicture()
        } else {
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = modifier, floatingActionButton = {
            FloatingActionButton(onClick = { showBottomSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }

        }) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            ProgressBar(
                todayTasks,
                dailyViewModel,
                context,
                permissionLauncher,
                { showImageOptionsSheet = true })

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp), state = listState
            ) {
                items(todayTasks, key = { it.id }) { task ->
                    TaskRow(
                        task = task, modifier = Modifier.animateItem(), onToggle = {
                            scrollToTop = true
                            taskViewModel.toggleTaskCompletion(task)
                        })
                }
            }
            // Scroll to top after list updates when a task is marked as done
            LaunchedEffect(todayTasks) {
                if (scrollToTop) {
                    listState.animateScrollToItem(0)
                    scrollToTop = false
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
        ImageOptionsBottomSheet(
            showBottomSheet = showImageOptionsSheet,
            onDismiss = { showImageOptionsSheet = false },
            dailyViewModel = dailyViewModel,
            context = context,
            permissionLauncher = permissionLauncher
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
    task: Task, modifier: Modifier = Modifier, onToggle: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onToggle() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.isCompleted, onCheckedChange = { onToggle() })
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
        if (task.sourceRecurringId != null) {
            Icon(Icons.Filled.Refresh, contentDescription = "Take Photo")
        }
    }
}


@Composable
fun ProgressBar(
    tasks: List<Task>,
    dailyViewModel: DailyViewModel,
    context: android.content.Context,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    onImageClick: () -> Unit
) {
    val completed = tasks.count { it.isCompleted }
    val total = tasks.size
    val progress = if (total > 0) completed / total.toFloat() else 0f
    var dayText by rememberSaveable { mutableStateOf("") }
    val savedDayText by dailyViewModel.noteForToday
    val bitmap by dailyViewModel.bitmap
    val coroutineScope = rememberCoroutineScope()

    // Load existing note
    LaunchedEffect(Unit) {
        dailyViewModel.loadNoteForToday()
    }

    LaunchedEffect(savedDayText) {
        dayText = savedDayText
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .heightIn(max = 170.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "$completed/$total tasks completed")
                TimeLeftInDayCounter()
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = ProgressIndicatorDefaults.linearColor,
                trackColor = ProgressIndicatorDefaults.linearTrackColor,

                )

            // Spacer for visual breathing room
            Row(
                modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
            ) {
                val maxLines = 5
                TextField(
                    value = dayText,
                    onValueChange = { newText ->
                        val lines = newText.count { it == '\n' } + 1
                        if (lines <= maxLines) {
                            dayText = newText
                            coroutineScope.launch {
                                delay(2000)
                                dailyViewModel.updateNoteForToday(newText)
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 100.dp),
                    placeholder = { Text("Enter notes and take a photo of your day here...") },
                    maxLines = Int.MAX_VALUE,
                    singleLine = false
                )

                Spacer(modifier = Modifier.width(8.dp))

                bitmap?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Captured Image",
                        modifier = Modifier
                            .size(84.dp)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onImageClick() },
                        contentScale = ContentScale.Crop
                    )
                } ?: Button(
                    onClick = {
                        when (ContextCompat.checkSelfPermission(
                            context, Manifest.permission.CAMERA
                        )) {
                            PackageManager.PERMISSION_GRANTED -> dailyViewModel.takePicture()
                            else -> permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }, modifier = Modifier
                        .padding(4.dp)
                        .height(64.dp)
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = "Take Photo")
                }
            }

        }
    }
}

@Composable
fun TimeLeftInDayCounter() {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    // Update every second
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000)
        }
    }
    val now = remember(currentTime) { LocalTime.now() }
    val secondsInDay = 24 * 60 * 60
    val secondsNow = now.toSecondOfDay()
    val secondsLeft = secondsInDay - secondsNow

    val hours = secondsLeft / 3600
    val minutes = (secondsLeft % 3600) / 60
    val seconds = secondsLeft % 60


    Column(
        modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End
    ) {
        Text(
            text = String.format("%02d:%02d:%02d", hours, minutes, seconds),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageOptionsBottomSheet(
    showBottomSheet: Boolean,
    onDismiss: () -> Unit,
    dailyViewModel: DailyViewModel,
    context: android.content.Context,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<String>
) {
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    text = "Image Options",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Button(
                    onClick = {
                        // Retake photo
                        when (ContextCompat.checkSelfPermission(
                            context, Manifest.permission.CAMERA
                        )) {
                            PackageManager.PERMISSION_GRANTED -> dailyViewModel.takePicture()
                            else -> permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                        onDismiss()
                    }, modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text("Retake")
                }
                Button(
                    onClick = {
                        // Delete photo
                        dailyViewModel.clearImage()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}