package se.umu.c22jwg.gritly.data

import kotlinx.coroutines.flow.Flow

class DailyEntryRepository(private val dailyEntryDao: DailyEntryDao) {

    val allEntries: Flow<List<DailyEntry>> = dailyEntryDao.getDailyEntries()

    suspend fun insertEntry(entry: DailyEntry) = dailyEntryDao.addDailyEntry(entry)
}