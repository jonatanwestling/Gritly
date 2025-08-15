package se.umu.c22jwg.gritly.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Configuration
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
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
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
 * TodayScreen displays the tasks for today, allowing the user to mark tasks as completed,
 * add new tasks, and view progress for the day. The user can also take a photo and add some notes
 * for the day.
 *
 * @param taskViewModel The ViewModel that provides the task data and handles task operations.
 * @param dailyViewModel The ViewModel that handles the daily entries with pictures and comments.
 */
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    taskViewModel: TaskViewModel,
    dailyViewModel: DailyViewModel,
) {
    val today = LocalDate.now().toString()
    val todayTasks by taskViewModel.todayTasks.observeAsState(emptyList())
    val listState = rememberLazyListState()
    var scrollToTop by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var showImageOptionsSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current
    // request permission to use the camera
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            dailyViewModel.takePicture()
        } else {
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // detect orientation
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showBottomSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }) { innerPadding ->
        // in laandscape we put the progressbar to the left and tasks to the right
        if (isLandscape) {
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                ProgressBar(
                    tasks = todayTasks,
                    dailyViewModel = dailyViewModel,
                    context = context,
                    permissionLauncher = permissionLauncher,
                    onImageClick = { showImageOptionsSheet = true },
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp)
                )
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp), state = listState
                ) {
                    items(todayTasks, key = { it.id }) { task ->
                        TaskRow(
                            task = task, modifier = Modifier.animateItem(), onToggle = {
                                scrollToTop = true
                                taskViewModel.toggleTaskCompletion(task)
                            })
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                ProgressBar(
                    tasks = todayTasks,
                    dailyViewModel = dailyViewModel,
                    context = context,
                    permissionLauncher = permissionLauncher,
                    onImageClick = { showImageOptionsSheet = true },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
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
            }
        }

        // scroll to top after list updates when a task is marked as done
        LaunchedEffect(todayTasks) {
            if (scrollToTop) {
                listState.animateScrollToItem(0)
                scrollToTop = false
            }
        }

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
 * TaskRow displays a single task with a checkbox to mark it as completed,
 * the task title, and an icon if the task is recurring.
 *
 * @param task The Task object to display.
 * @param modifier Modifier to apply to the row for animate item.
 * @param onToggle Callback invoked when the checkbox is toggled.
 */
@Composable
fun TaskRow(
    task: Task, modifier: Modifier = Modifier, onToggle: () -> Unit
) {
    // detect orientation to adjust padding
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val padding = if (isLandscape) 4.dp else 8.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(padding)
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
            Icon(Icons.Filled.Refresh, contentDescription = "Recurring Task")
        }
    }
}

/**
 * ProgressBar displays the progress of tasks for today, including a linear progress indicator and
 * additionally a text field for notes, and an image button to take a photo of the day.
 * It also includes a time counter showing the time left in the day.
 *
 * @param tasks The list of tasks for today.
 * @param dailyViewModel The ViewModel that handles the daily entries with pictures and comments.
 * @param context The current context for permission requests and toasts.
 * @param permissionLauncher The launcher for requesting camera permissions.
 * @param onImageClick Callback invoked when the image is clicked to show options.
 * @param modifier Modifier to apply to the card.
 */
@Composable
fun ProgressBar(
    tasks: List<Task>,
    dailyViewModel: DailyViewModel,
    context: android.content.Context,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    onImageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val completed = tasks.count { it.isCompleted }
    val total = tasks.size
    val progress = if (total > 0) completed / total.toFloat() else 0f
    var dayText by rememberSaveable { mutableStateOf("") }
    val savedDayText by dailyViewModel.noteForToday
    val bitmap by dailyViewModel.bitmap
    val coroutineScope = rememberCoroutineScope()

    // load existing note
    LaunchedEffect(Unit) {
        dailyViewModel.loadNoteForToday()
    }
    // update dayText when savedDayText changes
    LaunchedEffect(savedDayText) {
        dayText = savedDayText
    }

    Card(
        modifier = modifier.fillMaxWidth(),
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
            // progress bar under the text and time left counter
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = ProgressIndicatorDefaults.linearColor,
                trackColor = ProgressIndicatorDefaults.linearTrackColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            // text field for notes and image button to take a photo under the progress bar
            Row(
                modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = dayText,
                    onValueChange = { newText ->
                        dayText = newText
                        // slight delay to avoid database write on every keystroke
                        coroutineScope.launch {
                            delay(2000)
                            dailyViewModel.updateNoteForToday(newText)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 100.dp),
                    placeholder = { Text("Enter notes and take a photo of your day here...") },
                    maxLines = 5
                )
                Spacer(modifier = Modifier.width(8.dp))
                // display taken photo or a button to take a photo
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
                        // check for camera permission before taking a picture
                        when (ContextCompat.checkSelfPermission(
                            context, Manifest.permission.CAMERA
                        )) {
                            // take picture if permission is granted else request permission
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

/**
 * TimeLeftInDayCounter displays the time left in the current day in HH:MM:SS format. Placed at the
 * top right corner of the ProgressBar to easily let the user know how much time is left to complete
 * their tasks for the day.
 */
@SuppressLint("DefaultLocale")
@Composable
fun TimeLeftInDayCounter() {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000)
        }
    }
    // calculate time left in the day
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
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * ImageOptionsBottomSheet displays options for the image taken for the day
 * after a press of the captured image. It allows the user to retake the photo or delete it.
 *
 * @param showBottomSheet Boolean to control visibility of the bottom sheet.
 * @param onDismiss Callback invoked when the bottom sheet is dismissed.
 * @param dailyViewModel The ViewModel that handles the daily entries with pictures and comments.
 * @param context The current context for checking permissions.
 * @param permissionLauncher The launcher for requesting camera permissions.
 */
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
                        // retake photo, same process as taking a photo
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
                    // delete the image and clear the bitmap
                    onClick = {
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