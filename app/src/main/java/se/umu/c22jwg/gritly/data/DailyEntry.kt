package se.umu.c22jwg.gritly.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_entry")
data class DailyEntry(
    @PrimaryKey val date: String,
    val note: String,
    val imagePath: String?
)