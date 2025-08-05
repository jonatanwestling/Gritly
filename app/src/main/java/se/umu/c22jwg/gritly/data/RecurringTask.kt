package se.umu.c22jwg.gritly.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "recurring_tasks")
data class RecurringTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val title: String,

    // Each bit represents a day: Sun=bit 6, Mon=bit 5, ..., Sat=bit 0
    @ColumnInfo(name = "week_days")
    val weekDays: Int
)