package se.umu.c22jwg.gritly.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.*
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
import se.umu.c22jwg.gritly.utils.cleanDate

/**
 * PlanScreen displays a screen where users can view and manage their planned tasks. The screen
 * show a list of tasks planned for a specific date, defaults to the next day. User can look at
 * planned tasks or plan new tasks for a specific date that is selected by a calendar
 * (open source library). The user adds tasks to the selected date by pressing the add FAB. Users
 * can then add tasks or recurring tasks, which will then be displayed in the list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    taskViewModel: TaskViewModel,
    modifier: Modifier = Modifier,
) {
    // selected date defaults to tomorrow
    var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now().plusDays(1)) }
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(100) }
    val endMonth = remember { currentMonth.plusMonths(100) }
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }
    val daysOfWeek = remember { daysOfWeek(firstDayOfWeek = firstDayOfWeek) }
    // states for the calendar
    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek
    )
    val coroutineScope = rememberCoroutineScope()
    // bottom sheet states
    var showBottomSheet by remember { mutableStateOf(false) }
    var showCalendarSheet by remember { mutableStateOf(false) }
    val calendarSheetState = rememberModalBottomSheetState(
        // expand all the way
        skipPartiallyExpanded = true
    )

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    // adjust spacing based on orientation

    LaunchedEffect(showCalendarSheet) {
        if (showCalendarSheet) {
            coroutineScope.launch {
                calendarSheetState.expand()
            }
        }
    }
    // scroll to the selected month if it is in the grey area of the beginning of next month
    LaunchedEffect(selectedDate) {
        val targetMonth = YearMonth.from(selectedDate)
        if (state.firstVisibleMonth.yearMonth != targetMonth) {
            state.animateScrollToMonth(targetMonth)
        }
    }

    Scaffold(
        modifier = modifier, floatingActionButton = {
            FloatingActionButton(onClick = { showBottomSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }) { paddingValues ->
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // calendar toggle button
                Card(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .clickable { showCalendarSheet = true },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Selected: ${cleanDate(selectedDate.toString())}",
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
                Spacer(modifier = Modifier.width(16.dp))
                // Task list
                PlannedTaskList(
                    taskViewModel = taskViewModel,
                    selectedDate = selectedDate.toString(),
                    modifier = Modifier.weight(0.6f)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(paddingValues), horizontalAlignment = Alignment.CenterHorizontally
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
                            text = "Selected: ${cleanDate(selectedDate.toString())}",
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
                PlannedTaskList(
                    taskViewModel = taskViewModel, selectedDate = selectedDate.toString()
                )
            }
        }

        AddTaskBottomSheet(
            showBottomSheet = showBottomSheet,
            onDismiss = { showBottomSheet = false },
            taskViewModel = taskViewModel,
            title = "Add new task",
            selectedDate = selectedDate.toString()
        )

        if (showCalendarSheet) {
            ModalBottomSheet(
                onDismissRequest = { showCalendarSheet = false },
                sheetState = calendarSheetState,
                modifier = if (isLandscape) Modifier.fillMaxWidth(0.6f) else Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(
                        "Select a date to plan",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    SimpleCalendarTitle(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        currentMonth = state.firstVisibleMonth.yearMonth,
                        goToPrevious = {
                            coroutineScope.launch {
                                state.animateScrollToMonth(
                                    state.firstVisibleMonth.yearMonth.minusMonths(
                                        1
                                    )
                                )
                            }
                        },
                        goToNext = {
                            coroutineScope.launch {
                                state.animateScrollToMonth(
                                    state.firstVisibleMonth.yearMonth.plusMonths(
                                        1
                                    )
                                )
                            }
                        })
                    HorizontalCalendar(
                        state = state,
                        dayContent = { day ->
                            Day(day = day, isSelected = selectedDate == day.date, onClick = {
                                selectedDate = it.date
                                coroutineScope.launch {
                                    calendarSheetState.hide()
                                }.invokeOnCompletion {
                                    if (!calendarSheetState.isVisible) {
                                        showCalendarSheet = false
                                    }
                                }
                            })
                        },
                        monthHeader = { MonthHeader(daysOfWeek = daysOfWeek) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

/**
 * Composable function to handle the title and navigation of the calendar.
 * Displays the current month and allows navigation to the previous and next months.
 * Thhis function is from the open source library.
 *
 * @param modifier Modifier to apply to the title row.
 * @param currentMonth The currently displayed month in the calendar.
 * @param goToPrevious Callback to navigate to the previous month.
 * @param goToNext Callback to navigate to the next month.
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
        Text(
            text = currentMonth.month.getDisplayName(
                TextStyle.FULL, Locale.getDefault()
            ) + " ${currentMonth.year}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(onClick = goToNext) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Compose function to display the days at the top of the calendar like: Sun, Mon, Tue....
 * This function is from the open source library.
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
 * Composable function that is used to display the days in the calendar grid.
 * It displays the day number and handles selection and click events.
 * This function is from the open source library.
 *
 * @param day The CalendarDay object representing the day to display.
 * @param isSelected Boolean indicating if the day is selected.
 * @param onClick Callback function to handle click events on the day.
 */
@Composable
private fun Day(
    day: CalendarDay, isSelected: Boolean, onClick: (CalendarDay) -> Unit
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
                enabled = isSelectable, onClick = { onClick(day) }),
        contentAlignment = Alignment.Center) {
        Text(
            text = day.date.dayOfMonth.toString(), color = when {
                isSelected -> MaterialTheme.colorScheme.primary
                isOutOfMonth -> Color.Gray
                day.date.isAfter(today) -> MaterialTheme.colorScheme.onSurface
                else -> Color.Gray
            }, fontSize = 14.sp
        )
    }
}

/**
 * PlannedTaskList displays a list of tasks planned for a specific date.
 * It retrieves tasks from the TaskViewModel and filters them based on the selected date.
 * It also displays recurring tasks that are applicable for the selected date.
 *
 * @param taskViewModel The ViewModel that provides access to the tasks.
 * @param selectedDate The date for which tasks are to be displayed, formatted as a string
 * @param modifier Modifier to apply to the PlannedTaskList.
 */
@Composable
fun PlannedTaskList(
    taskViewModel: TaskViewModel, selectedDate: String, modifier: Modifier = Modifier
) {
    LaunchedEffect(selectedDate) {
        taskViewModel.setSelectedDate(selectedDate)
    }
    val allTasks by taskViewModel.allTasks.observeAsState(emptyList())
    val recurringTasks by taskViewModel.recurringTasks.observeAsState(emptyList())
    val filteredTasks = allTasks.filter { it.date == selectedDate }
    val filteredRecurringTasks =
        taskViewModel.getRecurringTasksForDate(selectedDate, recurringTasks)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "Planned Tasks (${filteredTasks.size + filteredRecurringTasks.size})",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (filteredTasks.isEmpty() && filteredRecurringTasks.isEmpty()) {
            Text(
                text = "No tasks planned for this date",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
            ) {
                items(filteredTasks, key = { it.id }) { task ->
                    PlannedTaskRow(task, taskViewModel)
                }
                items(filteredRecurringTasks, key = { it.id }) { recurringTask ->
                    PlannedRecurringTaskRow(recurringTask, taskViewModel)
                }
            }
        }
    }
}

/**
 * PlannedTaskRow displays a single planned task in the list.
 * It shows the task title and an icon to delete the task.
 *
 * @param task The Task object to display.
 * @param taskViewModel The ViewModel that provides access to the tasks.
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
                verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = task.title, style = MaterialTheme.typography.bodyLarge
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
                    .clickable { taskViewModel.deleteTask(task) })
        }
    }
}

/**
 * PlannedRecurringTaskRow displays a single planned recurring task in the list.
 * It shows the recurring task title and an icon to delete the recurring task as well as
 * an icon indicating that it is a recurring task.
 *
 * @param recurringTask The RecurringTask object to display.
 * @param taskViewModel The ViewModel that provides access to the tasks.
 */
@Composable
fun PlannedRecurringTaskRow(recurringTask: RecurringTask, taskViewModel: TaskViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)
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
                    .clickable { taskViewModel.deleteTaskFromId(recurringTask.id) })
        }
    }
}