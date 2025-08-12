package se.umu.c22jwg.gritly.ui.screens

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kizitonwose.calendar.compose.CalendarLayoutInfo
import com.kizitonwose.calendar.compose.HeatMapCalendar
import com.kizitonwose.calendar.compose.heatmapcalendar.HeatMapCalendarState
import com.kizitonwose.calendar.compose.heatmapcalendar.HeatMapWeek
import com.kizitonwose.calendar.compose.heatmapcalendar.rememberHeatMapCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.core.yearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import se.umu.c22jwg.gritly.data.Task
import se.umu.c22jwg.gritly.ui.viewmodel.PictureViewModel
import se.umu.c22jwg.gritly.ui.viewmodel.TaskViewModel
import se.umu.c22jwg.gritly.utils.cleanDate
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.*
private enum class Level(val color: Color) {
    Zero(Color(0xFFEBEDF0)),
    One(Color(0xFF9BE9A8)),
    Two(Color(0xFF40C463)),
    Three(Color(0xFF30A14E)),
    Four(Color(0xFF216E3A)),
}


/**
 * HistoryScreen is a composable function that displays a feed of past daily entries with their
 * associated tasks.It allows users to view completed tasks for each date and shows a bottom sheet
 * with detailed task information when a card is clicked.
 *
 * @param taskViewModel The ViewModel that provides the task data and handles task operations.
 * @param pictureViewModel The ViewModel that handles the daily entries and pictures.
 */
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HistoryScreen(taskViewModel: TaskViewModel, pictureViewModel: PictureViewModel) {
    val entries by pictureViewModel.allEntries
    val bitmap by pictureViewModel.bitmap
    val allTasks by taskViewModel.allTasks.collectAsState()

    // state variables for the bottom sheet
    var selectedDateForSheet by remember { mutableStateOf<String?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }

    // load all daily entries when the screen is launched
    LaunchedEffect(Unit) {
        pictureViewModel.loadAllEntries()
    }

    // group tasks by date
    val tasksByDate = remember(allTasks) {
        allTasks.groupBy { it.date }
    }

    val endDate = remember { LocalDate.now() }
    // GitHub only shows contributions for the past 12 months
    val startDate = remember { endDate.minusMonths(12) }

    val data: Map<LocalDate, Level> = remember(tasksByDate, startDate, endDate) {
        buildMap {
            for ((dateStr, tasks) in tasksByDate) {
                val total = tasks.size
                if (total == 0) continue
                val completed = tasks.count { it.isCompleted }
                val percent = (completed * 100) / total
                val level = when {
                    percent <= 0 -> Level.Zero
                    percent <= 25 -> Level.One
                    percent <= 50 -> Level.Two
                    percent <= 75 -> Level.Three
                    else -> Level.Four
                }
                val date = try {
                    LocalDate.parse(dateStr)
                } catch (e: Exception) {
                    continue
                }
                if (date >= startDate && date <= endDate) {
                    put(date, level)
                }
            }
        }
    }

    var selection by remember { mutableStateOf<Pair<LocalDate, Level>?>(null) }

    Scaffold {
        Column {
            val state = rememberHeatMapCalendarState(
                startMonth = startDate.yearMonth,
                endMonth = endDate.yearMonth,
                firstVisibleMonth = endDate.yearMonth,
                firstDayOfWeek = firstDayOfWeekFromLocale(),
            )
            HeatMapCalendar(
                modifier = Modifier.padding(vertical = 10.dp),
                state = state,
                contentPadding = PaddingValues(end = 6.dp),
                dayContent = { day, week ->
                    Day(
                        day = day,
                        startDate = startDate,
                        endDate = endDate,
                        week = week,
                        level = data[day.date] ?: Level.Zero,
                    ) { clicked ->
                        selection = Pair(clicked, data[clicked] ?: Level.Zero)
                    }
                },
                weekHeader = { WeekHeader(it) },
                monthHeader = { MonthHeader(it, endDate, state) },
            )
            CalendarInfo(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 44.dp),
            )
            Box(modifier = Modifier.weight(1f)) {
                BottomContent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .align(Alignment.BottomCenter),
                    selection = selection,
                )
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
                    // calculate the percentage of completed tasks
                    val percentCompleted = if (totalTasks > 0) (completedTasks * 100 / totalTasks) else 0
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedDateForSheet = entry.date
                                showBottomSheet = true
                            },
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
                                    text = cleanDate(entry.date),
                                    style = MaterialTheme.typography.titleLarge,
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Completed: $percentCompleted% ($completedTasks / $totalTasks)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                // show note just if it is not empty
                                if (entry.note.isNotBlank()) {
                                    Text(
                                        text = entry.note,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                            // show the image if the bitmap is not null
                            if (entry.imagePath != null)
                                AsyncImage(
                                    model = File(entry.imagePath),
                                    contentDescription = "Daily photo",
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
        if (showBottomSheet) {
            TaskBottomSheet(
                onDismiss = { showBottomSheet = false },
                tasks = tasksByDate[selectedDateForSheet] ?: emptyList(),
                title = "Tasks for ${cleanDate(selectedDateForSheet!!)}"
            )
        }
    }
}

private val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)

@Composable
private fun BottomContent(
    modifier: Modifier = Modifier,
    selection: Pair<LocalDate, Level>? = null,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (selection != null) {
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(text = "Clicked: ${formatter.format(selection.first)}")
                LevelBox(color = selection.second.color)
            }
        }
    }
}

@Composable
private fun CalendarInfo(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(text = "Less", fontSize = 10.sp)
        Level.entries.forEach { level ->
            LevelBox(level.color)
        }
        Text(text = "More", fontSize = 10.sp)
    }
}

private val daySize = 18.dp

@Composable
private fun Day(
    day: CalendarDay,
    startDate: LocalDate,
    endDate: LocalDate,
    week: HeatMapWeek,
    level: Level,
    onClick: (LocalDate) -> Unit,
) {
    // We only want to draw boxes on the days that are in the
    // past 12 months. Since the calendar is month-based, we ignore
    // the future dates in the current month and those in the start
    // month that are older than 12 months from today.
    // We draw a transparent box on the empty spaces in the first week
    // so the items are laid out properly as the column is top to bottom.
    val weekDates = week.days.map { it.date }
    if (day.date in startDate..endDate) {
        LevelBox(level.color) { onClick(day.date) }
    } else if (weekDates.contains(startDate)) {
        LevelBox(Color.Transparent)
    }
}

@Composable
private fun LevelBox(color: Color, onClick: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .size(daySize) // Must set a size on the day.
            .padding(2.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color = color)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
    )
}

@Composable
private fun WeekHeader(dayOfWeek: DayOfWeek) {
    Box(
        modifier = Modifier
            .height(daySize) // Must set a height on the day of week so it aligns with the day.
            .padding(horizontal = 4.dp),
    ) {
        Text(
            text = dayOfWeek.displayText(),
            modifier = Modifier.align(Alignment.Center),
            fontSize = 10.sp,
        )
    }
}

fun YearMonth.displayText(short: Boolean = false): String {
    return "${this.month.displayText(short = short)} ${this.year}"
}

fun Month.displayText(short: Boolean = true): String {
    val style = if (short) TextStyle.SHORT else TextStyle.FULL
    return getDisplayName(style, Locale.ENGLISH)
}

fun DayOfWeek.displayText(uppercase: Boolean = false, narrow: Boolean = false): String {
    val style = if (narrow) TextStyle.NARROW else TextStyle.SHORT
    return getDisplayName(style, Locale.ENGLISH).let { value ->
        if (uppercase) value.uppercase(Locale.ENGLISH) else value
    }
}

@Composable
private fun MonthHeader(
    calendarMonth: CalendarMonth,
    endDate: LocalDate,
    state: HeatMapCalendarState,
) {
    val density = LocalDensity.current
    val firstFullyVisibleMonth by remember {
        // Find the first index with at most one box out of bounds.
        derivedStateOf { getMonthWithYear(state.layoutInfo, daySize, density) }
    }
    if (calendarMonth.weekDays.first().first().date <= endDate) {
        val month = calendarMonth.yearMonth
        val title = if (month == firstFullyVisibleMonth) {
            month.displayText(short = true)
        } else {
            month.month.displayText()
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 1.dp, start = 2.dp),
        ) {
            Text(text = title, fontSize = 10.sp)
        }
    }
}

// Find the first index with at most one box out of bounds.
private fun getMonthWithYear(
    layoutInfo: CalendarLayoutInfo,
    daySize: Dp,
    density: Density,
): YearMonth? {
    val visibleItemsInfo = layoutInfo.visibleMonthsInfo
    return when {
        visibleItemsInfo.isEmpty() -> null
        visibleItemsInfo.count() == 1 -> visibleItemsInfo.first().month.yearMonth
        else -> {
            val firstItem = visibleItemsInfo.first()
            val daySizePx = with(density) { daySize.toPx() }
            if (
            // Ensure the Month + Year text can fit.
                firstItem.size < daySizePx * 3 ||
                // Ensure the week row size - 1 is visible.
                firstItem.offset < layoutInfo.viewportStartOffset &&
                (layoutInfo.viewportStartOffset - firstItem.offset > daySizePx)
            ) {
                visibleItemsInfo[1].month.yearMonth
            } else {
                firstItem.month.yearMonth
            }
        }
    }
}

/**
 * TaskBottomSheet is a composable function that displays a bottom sheet with a list of tasks for a specific date.
 * It shows the tasks in a LazyColumn displayed like in the today screen but greyed out to indicate that the past cant
 * be changed.
 *
 * @param onDismiss Callback to dismiss the bottom sheet.
 * @param tasks List of tasks to display in the bottom sheet.
 * @param title Title of the bottom sheet.
 * @param modifier Modifier for additional styling or layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskBottomSheet(
    onDismiss: () -> Unit,
    tasks: List<Task>,
    title: String,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState()
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
            // displays the tasks in a scrollable list as in the TodayScreen
            LazyColumn(
                modifier = Modifier.fillMaxHeight(0.6f)
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * TaskRow is a composable function that displays a single task in a row format.
 * This code is a slightly modified version of the TaskRow from the TodayScreen.
 * It is used in the TaskBottomSheet to show past tasks and does not allow any
 * interaction with the task.
 *
 * @param task The task to display.
 * @param modifier Modifier for additional styling or layout.
 */
@Composable
fun TaskRow(task: Task, modifier: Modifier = Modifier) {
    // Make the text grey so user understand it is disabled
    val textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    Row(
        modifier = modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.isCompleted,
            // Disable the the interaction with the checkbox
            onCheckedChange = null,
            enabled = false
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyLarge.copy(
                // Display the the text as normal
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                color = textColor
            )
        )
    }
}
