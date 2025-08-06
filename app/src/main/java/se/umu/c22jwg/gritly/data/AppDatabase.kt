package se.umu.c22jwg.gritly.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Task::class, RecurringTask::class, DailyEntry::class], version = 13, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun recurringTaskDao(): RecurringTaskDao

    abstract fun dailyEntryDao(): DailyEntryDao
}