package se.umu.c22jwg.gritly.data

class DailyEntryRepository(private val dailyEntryDao: DailyEntryDao) {

    suspend fun getAllEntries(): List<DailyEntry> {
        return dailyEntryDao.getDailyEntries()
    }
    suspend fun getEntryByDate(date: String): DailyEntry? {
        return dailyEntryDao.getEntryByDate(date)
    }
    suspend fun insertEntry(entry: DailyEntry) = dailyEntryDao.addDailyEntry(entry)
}