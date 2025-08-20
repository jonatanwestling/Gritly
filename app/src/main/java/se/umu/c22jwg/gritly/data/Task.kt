package se.umu.c22jwg.gritly.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val title: String,

    val date: String,

    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,
    // to keep track if it is a recurring task
    @ColumnInfo(name = "source_recurring_id")
    val sourceRecurringId: Long? = null
)