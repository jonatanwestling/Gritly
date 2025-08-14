package se.umu.c22jwg.gritly.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "recurring_tasks")
data class RecurringTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val title: String,

    // each bit represents a day: Mon=bit 6, ..., Sun=bit 0
    @ColumnInfo(name = "week_days")
    val weekDays: Int
)