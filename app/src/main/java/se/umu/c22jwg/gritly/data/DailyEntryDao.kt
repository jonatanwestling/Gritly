package se.umu.c22jwg.gritly.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyEntryDao {
    @Query("SELECT * FROM daily_entry ORDER BY date DESC")
    suspend fun getDailyEntries(): List<DailyEntry>

    @Query("SELECT * FROM daily_entry WHERE date = :date")
    suspend fun getEntryByDate(date: String): DailyEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addDailyEntry(dailyEntry: DailyEntry)
}