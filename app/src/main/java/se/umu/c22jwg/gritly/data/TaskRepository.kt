package se.umu.c22jwg.gritly.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao,
    private val recurringTaskDao: RecurringTaskDao
) {
    val getAllTasks: Flow<List<Task>> = taskDao.getAllTasks()
    val recurringTasks: Flow<List<RecurringTask>> = recurringTaskDao.getRecurringTasks()

    suspend fun insertTask(task: Task) = taskDao.insert(task)
    suspend fun updateTask(task: Task) = taskDao.update(task)
    suspend fun deleteTask(task: Task) = taskDao.delete(task)

    suspend fun getTaskByRecurringSource(date: String, sourceId: Long): Task? {
        return taskDao.getTaskByRecurringSource(date, sourceId)
    }

    suspend fun insertRecurringTask(recurringTask: RecurringTask) = recurringTaskDao.insert(recurringTask)
    suspend fun deleteRecurringTaskById(id: Long) = recurringTaskDao.deleteWithId(id)
}