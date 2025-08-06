package se.umu.c22jwg.gritly.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import java.time.LocalDate

@Entity(tableName = "daily_entry")
data class DailyEntry(
    @PrimaryKey val date: LocalDate,
    val note: String,
    val imagePath: String
)