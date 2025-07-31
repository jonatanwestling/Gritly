package se.umu.c22jwg.gritly.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import se.umu.c22jwg.gritly.ui.theme.GritlyTheme

data class GoalItem(val id: Int, val title: String, var isDone: Boolean)

@Composable
fun TodayScreen() {
    // Mock data
    var goals by remember {
        mutableStateOf(
            listOf(
                GoalItem(1, "Workout", false),
                GoalItem(2, "Read 30 pagesRead 30 pagesRead 30 pagesRead 30 pagesRead 30 pagesRead 30 pages", false),
                GoalItem(3, "Meditate 10 min", false),
                GoalItem(4, "Workout", false),
                GoalItem(5, "Read 30 pages", false),
                GoalItem(6, "Meditate 10 min", false),
                GoalItem(7, "Workout", false),
                GoalItem(8, "Read 30 pages", false),
                GoalItem(9, "Meditate 10 min", false),
                GoalItem(10, "Workout", false),
                GoalItem(11, "Read 30 pages", false),
                GoalItem(12, "Meditate 10 min", false)
            )
        )
    }

    Column {
        progressBar()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            items(goals, key = { it.id }) { goal ->
                GoalRow(
                    goal = goal,
                    modifier = Modifier.animateItem(),
                    onToggle = {
                        goals = updateAndSortGoals(goals, goal.copy(isDone = !goal.isDone))
                    }
                )
            }
        }
    }
}

/**
 * Function that renders the GoalRow inside the list. It is a row of text to the left and
 * a checkbox to the right. The whole row is clickable.
 *
 * @param goal The GoalItem object representing the data for this row.
 * @param modifier Modifier to be applied to the row container.
 * @param onToggle Lambda invoked when either the checkbox or row is clicked to toggle completion.
 */
@Composable
fun GoalRow(
    goal: GoalItem,
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
            text = goal.title,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            style = if (goal.isDone) {
                LocalTextStyle.current.copy(textDecoration = TextDecoration.LineThrough)
            } else {
                LocalTextStyle.current
            }
        )
        Checkbox(
            checked = goal.isDone,
            onCheckedChange = { onToggle() }
        )
    }
}

fun updateAndSortGoals(
    goals: List<GoalItem>,
    updatedGoal: GoalItem
): List<GoalItem> {
    val updated = goals.map {
        if (it.id == updatedGoal.id) updatedGoal else it
    }

    return buildList {
        addAll(updated.filter { !it.isDone })
        addAll(updated.filter { it.isDone && it.id == updatedGoal.id })
        addAll(updated.filter { it.isDone && it.id != updatedGoal.id })
    }
}

@Composable
fun progressBar() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "This is a card")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GritlyTheme {
        TodayScreen()
    }
}