package se.umu.c22jwg.gritly.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val title: String,

    val date: String, // ISO format, e.g., "2024-06-07"

    val description: String? = null,

    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false
)