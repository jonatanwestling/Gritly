package se.umu.c22jwg.gritly.ui.screens

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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

            // Show planned tasks for selected date
            PlannedTasksList(taskViewModel = taskViewModel, selectedDate = selectedDate.toString())
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

@Composable
fun PlannedTasksList(taskViewModel: TaskViewModel, selectedDate: String) {
    val allTasks by taskViewModel.tasks.collectAsState()
    val plannedTasks = allTasks.filter { it.date == selectedDate }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "Planned Tasks (${plannedTasks.size})",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        if (plannedTasks.isEmpty()) {
            Text(
                text = "No tasks planned for this date",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 200.dp)
            ) {
                items(plannedTasks, key = { it.id }) { task ->
                    PlannedTaskRow(task = task)
                }
            }
        }
    }
}

@Composable
fun PlannedTaskRow(task: Task) {
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            
            if (task.isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddGoalScreenPreview() {
    GritlyTheme {
        Text("AddGoalScreen Preview - Requires TaskViewModel")
    }
}