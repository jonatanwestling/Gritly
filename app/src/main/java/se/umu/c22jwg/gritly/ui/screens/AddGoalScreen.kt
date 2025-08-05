package se.umu.c22jwg.gritly.ui.screens

import android.R
import android.media.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.*
import se.umu.c22jwg.gritly.ui.theme.GritlyTheme
import se.umu.c22jwg.gritly.ui.viewmodel.TaskViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek

import se.umu.c22jwg.gritly.data.Task
import se.umu.c22jwg.gritly.data.RecurringTask
import se.umu.c22jwg.gritly.ui.components.AddTaskBottomSheet
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGoalScreen(
    taskViewModel: TaskViewModel,
    modifier: Modifier = Modifier,
) {
    var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now().plusDays(1)) }

    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(100) }
    val endMonth = remember { currentMonth.plusMonths(100) }
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }
    val daysOfWeek = remember { daysOfWeek(firstDayOfWeek = firstDayOfWeek) }
    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek
    )
    val coroutineScope = rememberCoroutineScope()

    // Bottom sheet states
    var showBottomSheet by remember { mutableStateOf(false) }
    var showCalendarSheet by remember { mutableStateOf(false) }
    val calendarSheetState = rememberModalBottomSheetState()

    // Scroll to the selected 
    LaunchedEffect(selectedDate) {
        val targetMonth = YearMonth.from(selectedDate)
        if (state.firstVisibleMonth.yearMonth != targetMonth) {
            state.animateScrollToMonth(targetMonth)
        }
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = { showBottomSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Calendar toggle button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { showCalendarSheet = true },
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Selected: ${selectedDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Select date",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Show planned recurring tasks and normal tasks for selected date
            UnifiedPlannedTasksList(taskViewModel = taskViewModel, selectedDate = selectedDate.toString())
        }
        // AddTaskBottomSheet component
        AddTaskBottomSheet(
            showBottomSheet = showBottomSheet,
            onDismiss = { showBottomSheet = false },
            taskViewModel = taskViewModel,
            title = "Add new task",
            selectedDate = selectedDate.toString()
        )
        // Calendar BottomSheet
        if (showCalendarSheet) {
            ModalBottomSheet(
                onDismissRequest = { showCalendarSheet = false },
                sheetState = calendarSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "Select Date",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    // Header title with nav buttons for the calendar 
                    SimpleCalendarTitle(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        currentMonth = state.firstVisibleMonth.yearMonth,
                        goToPrevious = {
                            coroutineScope.launch {
                                state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.minusMonths(1))
                            }
                        },
                        goToNext = {
                            coroutineScope.launch {
                                state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.plusMonths(1))
                            }
                        }
                    )
                    // Horizontal calendar provided by the Calendar open source library
                    HorizontalCalendar(
                        state = state,
                        dayContent = { day ->
                            Day(
                                day = day,
                                isSelected = selectedDate == day.date,
                                onClick = { 
                                    selectedDate = it.date
                                    coroutineScope.launch {
                                        calendarSheetState.hide()
                                    }.invokeOnCompletion {
                                        if (!calendarSheetState.isVisible) {
                                            showCalendarSheet = false
                                        }
                                    }
                                }
                            )
                        },
                        monthHeader = {
                            MonthHeader(daysOfWeek = daysOfWeek)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

/**
 * Simple calendar title with nav buttons for the calendar,
 * and the current month and year. This is used in the calendar bottom sheet.
 *
 * @param modifier Modifier for the calendar title
 * @param currentMonth The current month and year
 * @param goToPrevious Function to go to the previous month
 * @param goToNext Function to go to the next month
 */
@Composable
private fun SimpleCalendarTitle(
    modifier: Modifier = Modifier,
    currentMonth: YearMonth,
    goToPrevious: () -> Unit,
    goToNext: () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = goToPrevious) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(24.dp)
            )
        }
        // month in the middle
        Text(
            text = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) +
                    " ${currentMonth.year}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(onClick = goToNext) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Function to display the name of the days of the week in the calendar header.
 * This is used in the calendar bottom sheet. 
 *
 * @param daysOfWeek The days of the week
 */
@Composable
private fun MonthHeader(daysOfWeek: List<DayOfWeek>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        for (dayOfWeek in daysOfWeek) {
            Text(
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 15.sp,
                text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}


/**
 * Function to display the days of the month in the calendar.
 * This is used in the calendar bottom sheet. And the code is provided by the Calendar open source library.
 *
 * @param day The day to display
 * @param isSelected Whether the day is selected
 * @param onClick Function to call when the day is clicked
 */
@Composable
private fun Day(
    day: CalendarDay,
    isSelected: Boolean,
    onClick: (CalendarDay) -> Unit
) {
    val today = LocalDate.now()
    val isSelectable = day.position == DayPosition.MonthDate && day.date.isAfter(today)
    val isOutOfMonth = day.position == DayPosition.InDate || day.position == DayPosition.OutDate

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(6.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else Color.Transparent
            )
            .clickable(
                enabled = isSelectable,
                onClick = { onClick(day) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            color = when {
                isSelected -> MaterialTheme.colorScheme.primary
                isOutOfMonth -> Color.Gray // Grey out in-month and out-of-month filler days
                day.date.isAfter(today) -> MaterialTheme.colorScheme.onSurface
                else -> Color.Gray // Today and previous days are grey
            },
            fontSize = 14.sp
        )
    }
}

/**
 * Displays the planned tasks for a specific date. It displays both normal tasks and recurring tasks.
 * It combines the tasks into a unified list, where each item is either a Task or a RecurringTask.
 * 
 * @param taskViewModel The ViewModel that provides access to tasks
 * @param selectedDate The date for which to display planned tasks, formatted as "yyyy-MM-dd"
 */
@Composable
fun UnifiedPlannedTasksList(taskViewModel: TaskViewModel, selectedDate: String) {
    // Collect all tasks from the ViewModel
    val allTasks by taskViewModel.allTasks.collectAsState()
    val recurringTasks by taskViewModel.recurringTasks.collectAsState()
    // Filter tasks for the selected date
    val plannedTasks = allTasks.filter { it.date == selectedDate }
    // Collect recurring tasks for the selected date
    val recurringTaskIdsInTasks = plannedTasks.mapNotNull { it.sourceRecurringId }.toSet()
    // Get recurring tasks that are planned for the selected date
    val plannedRecurringTasks = taskViewModel.getRecurringTasksForDate(selectedDate, recurringTasks)
        .filter { it.id !in recurringTaskIdsInTasks }

    // Build unified list as pairs
    val unifiedList: List<Pair<Task?, RecurringTask?>> =
        plannedTasks.map { it to null } + plannedRecurringTasks.map { null to it }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // title and count of planned tasks
        Text(
            text = "Planned Tasks (${unifiedList.size})",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (unifiedList.isEmpty()) {
            Text(
                text = "No tasks planned for this date",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
            ) {
                items(unifiedList) { (task, recurringTask) ->
                    if (task != null) {
                        PlannedTaskRow(task, taskViewModel)
                    } else if (recurringTask != null) {
                        PlannedRecurringTaskRow(recurringTask, taskViewModel)
                    }
                }
            }
        }
    }
}

/**
 * Displays a row for a planned task. It includes the task title, an icon for recurring tasks,
 * and a delete icon to remove the task.
 *
 * @param task The Task object to display
 * @param taskViewModel The ViewModel that provides access to tasks
 */
@Composable
fun PlannedTaskRow(task: Task, taskViewModel: TaskViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                )

                if (task.sourceRecurringId != null) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Recurring task",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .size(28.dp)
                            .padding(start = 8.dp)
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .size(24.dp)
                    .clickable {
                        taskViewModel.deleteTask(task)
                    }
            )
        }
    }
}
/**
 * Displays a row for a planned recurring task. It includes the task title, an icon for recurring tasks,
 * and a delete icon to remove the task.
 *
 * @param recurringTask The RecurringTask object to display
 * @param taskViewModel The ViewModel that provides access to tasks
 */
@Composable
fun PlannedRecurringTaskRow(recurringTask: RecurringTask, taskViewModel: TaskViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Recurring task",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 8.dp)
            )
            Text(
                text = recurringTask.title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
        }
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .size(24.dp)
                    .clickable {
                        taskViewModel.deleteTaskFromId(recurringTask.id)
                    }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddGoalScreenPreview() {
    GritlyTheme {
        Text("Hey")
    }
}