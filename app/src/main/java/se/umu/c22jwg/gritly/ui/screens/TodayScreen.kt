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
    //Mock data
    var goals by remember {
        mutableStateOf(
            listOf(
                GoalItem(1, "Workout", false),
                GoalItem(2, "Read 30 pages", false),
                GoalItem(3, "Meditate 10 min", false)
            )
        )
    }

    // Make it a scrollable list
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        items(goals, key = { it.id }) { goal ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding( 8.dp)
                    .clickable {
                        goals = goals.map {
                            if (it.id == goal.id) it.copy(isDone = !it.isDone) else it
                        }
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = goal.title,
                    // Make the text have a line through if it is done
                    style = if (goal.isDone) {
                        LocalTextStyle.current.copy(textDecoration = TextDecoration.LineThrough)
                    } else {
                        LocalTextStyle.current
                    })
                Checkbox(
                    checked = goal.isDone,
                    onCheckedChange = { checked ->
                        goals = goals.map {
                            if (it.id == goal.id) it.copy(isDone = checked) else it
                        }
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GritlyTheme {
        MainScreen()
    }
}