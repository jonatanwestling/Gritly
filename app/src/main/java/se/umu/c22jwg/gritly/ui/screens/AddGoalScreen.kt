package se.umu.c22jwg.gritly.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import se.umu.c22jwg.gritly.ui.theme.GritlyTheme
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun AddGoalScreen(
    modifier: Modifier = Modifier,
    onPlanTomorrow: () -> Unit = {},
    onPlanAnotherDay: (LocalDate) -> Unit = {}
) {
    // State for selected date
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    // Calendar configuration
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(100) }
    val endMonth = remember { currentMonth.plusMonths(100) }
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }
    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Calendar
        HorizontalCalendar(
            state = state,
            dayContent = { day ->
                Day(
                    day = day,
                    isSelected = selectedDate == day.date,
                    onClick = { selectedDate = it.date }
                )
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Instructions
        Text(
            text = "Plan your goals for a day",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Buttons
        Button(
            onClick = {
                onPlanTomorrow()
                selectedDate = LocalDate.now().plusDays(1) // Select tomorrow
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Plan tomorrow")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { selectedDate?.let { onPlanAnotherDay(it) } },
            enabled = selectedDate != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Plan another day")
        }

        // Display selected date (optional, for feedback)
        selectedDate?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Selected: $it",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
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
    Box(
        modifier = Modifier
            .aspectRatio(1f) // Square days
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else Color.Transparent
            )
            .clickable(
                enabled = day.position == DayPosition.MonthDate, // Only allow clicks on current month's days
                onClick = { onClick(day) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            color = when {
                isSelected -> MaterialTheme.colorScheme.primary
                day.position != DayPosition.MonthDate -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AddGoalScreenPreview() {
    GritlyTheme {
        AddGoalScreen()
    }
}