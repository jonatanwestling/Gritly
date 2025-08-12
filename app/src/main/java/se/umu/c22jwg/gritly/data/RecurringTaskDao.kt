package se.umu.c22jwg.gritly.data
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTaskDao {
    @Query("SELECT * FROM recurring_tasks ORDER BY id DESC")
    fun getRecurringTasks(): Flow<List<RecurringTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: RecurringTask)

    @Update
    suspend fun update(task: RecurringTask)

    @Delete
    suspend fun delete(task: RecurringTask)

    @Query("DELETE FROM recurring_tasks WHERE id = :taskId")
    suspend fun deleteWithId(taskId: Long)

}