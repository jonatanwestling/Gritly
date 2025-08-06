package se.umu.c22jwg.gritly.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Handler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import se.umu.c22jwg.gritly.ui.theme.GritlyTheme
import se.umu.c22jwg.gritly.ui.viewmodel.TaskViewModel
import se.umu.c22jwg.gritly.data.Task
import se.umu.c22jwg.gritly.ui.components.AddTaskBottomSheet
import java.time.LocalDate
import kotlinx.coroutines.delay
import org.intellij.lang.annotations.JdkConstants
import se.umu.c22jwg.gritly.ui.viewmodel.PictureViewModel
import java.time.Duration
import java.time.LocalTime

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
    pictureViewModel: PictureViewModel,
    modifier: Modifier = Modifier,
    ) {

    val allTasks by taskViewModel.allTasks.collectAsState()
    val today = LocalDate.now().toString()
    val todayTasks by taskViewModel.todayTasks.collectAsState()
    val listState = rememberLazyListState()
    var scrollToTop by remember { mutableStateOf(false) }

    // Bottom sheet state
    var showBottomSheet by remember { mutableStateOf(false) }


    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                FloatingActionButton({ pictureViewModel.clearImage() }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Take Picture")
                }
                FloatingActionButton(onClick = { showBottomSheet = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task")
                }
            }
        }
    ) {
        Column (
            modifier = Modifier
            .fillMaxSize()
        ){

            ProgressBar(todayTasks, pictureViewModel.bitmap, pictureViewModel)



            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                state = listState
            ) {
                items(todayTasks, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        modifier = Modifier.animateItem(),
                        onToggle = {

                                scrollToTop = true

                            taskViewModel.toggleTaskCompletion(task)
                        }
                    )
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
fun ProgressBar(tasks: List<Task>, picture: MutableState<Bitmap>, pictureViewModel: PictureViewModel) {
    val completed = tasks.count { it.isCompleted }
    val total = tasks.size
    val progress = if (total > 0) completed / total.toFloat() else 0f
    var text by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ){
                Text(text = "$completed/$total tasks completed")
                Spacer(modifier = Modifier.width(72.dp))
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
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                    decorationBox = { innerTextField ->
                        Box {
                            if (text.isEmpty()) {
                                Text(
                                    text = "Enter your notes here...",
                                    color = Color.Gray
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                Spacer(modifier = Modifier.width(8.dp)) // Optional spacing

                if (pictureViewModel.hasPicture()) {
                    Image(
                        bitmap = pictureViewModel.bitmap.value.asImageBitmap(),
                        contentDescription = "Captured Image",
                        modifier = Modifier
                            .size(64.dp)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Button(
                        onClick = { pictureViewModel.takePicture() },
                        modifier = Modifier
                            .padding(4.dp)
                            .height(64.dp)
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = "Take Photo")
                    }
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
            modifier = Modifier
                .fillMaxWidth()
                    ,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = String.format("%02d:%02d:%02d", hours, minutes, seconds),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GritlyTheme {
        Text("TodayScreen Preview - Requires TaskViewModel")
    }
}