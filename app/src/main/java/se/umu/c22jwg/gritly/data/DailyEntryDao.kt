package se.umu.c22jwg.gritly.data

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyEntryDao {
    @Query("SELECT * FROM daily_entry ORDER BY date DESC")
    fun getDailyEntries(): Flow<List<DailyEntry>>
}