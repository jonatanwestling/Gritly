package se.umu.c22jwg.gritly.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "recurring_tasks")
data class RecurringTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val title: String,

    //Days with this format: everyday: 1111111, mondays: 1000000,
    // mondays and fridays: 1000100
    @ColumnInfo(name = "week_days")
    val weekDays: Int,
)