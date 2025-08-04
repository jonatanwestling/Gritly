package se.umu.c22jwg.gritly.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import se.umu.c22jwg.gritly.data.AppDatabase
import se.umu.c22jwg.gritly.data.RecurringTask
import se.umu.c22jwg.gritly.data.RecurringTaskDao
import se.umu.c22jwg.gritly.data.Task
import se.umu.c22jwg.gritly.data.TaskDao
import java.time.LocalDate

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase
        get() {
            if (db == null) {
                db = Room.databaseBuilder(
                    getApplication<Application>().applicationContext,
                    AppDatabase::class.java,
                    "gritly_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
            }
            return db!!
        }

    companion object {
        private var db: AppDatabase? = null
    }

    private val taskDao: TaskDao = database.taskDao()
    private val recurringTaskDao: RecurringTaskDao = database.recurringTaskDao()

    init {
        loadTasks()
    }

    // State for all tasks
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    // State for today's tasks
    private val _todayTasks = MutableStateFlow<List<Task>>(emptyList())
    val todayTasks: StateFlow<List<Task>> = _todayTasks.asStateFlow()

    // State for completed tasks
    private val _completedTasks = MutableStateFlow<List<Task>>(emptyList())
    val completedTasks: StateFlow<List<Task>> = _completedTasks.asStateFlow()

    private fun loadTasks() {
        viewModelScope.launch {
            taskDao.getAllTasks().collect { taskList ->
                val sortedTasks = taskList.sortedWith(
                    compareBy<Task> { it.isCompleted }
                        .thenByDescending { it.id }
                )
                _tasks.value = sortedTasks
                _todayTasks.value = sortedTasks.filter { !it.isCompleted }
                _completedTasks.value = sortedTasks.filter { it.isCompleted }
            }
        }

        viewModelScope.launch {
            val today = LocalDate.now().toString()
            recurringTaskDao.getRecurringTasks().collect { recurringTasks ->
                for (recurring in recurringTasks) {
                    val dayOfWeek = LocalDate.now().dayOfWeek.value // 1–7
                    val shouldRepeat = ((recurring.weekDays shr (dayOfWeek - 1)) and 1) == 1

                    if (shouldRepeat) {
                        val existing = taskDao.getTaskByRecurringSource(today, recurring.id)
                        if (existing == null) {
                            val newTask = Task(
                                title = recurring.title,
                                date = today,
                                isCompleted = false,
                                sourceRecurringId = recurring.id
                            )
                            taskDao.insert(newTask)
                        }
                    }
                }
            }
        }
    }

    fun addTask(title: String, date: String, description: String? = null) {
        viewModelScope.launch {
            val task = Task(
                title = title,
                date = date,
                description = description,
                isCompleted = false
            )
            taskDao.insert(task)
        }
    }

    fun addRecurringTask(title: String, startDate: String, description: String? = null) {
        viewModelScope.launch {
            val task = RecurringTask(
                title = title,
                weekDays = 1111111
            )
            recurringTaskDao.insert(task)
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            taskDao.update(task)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            if (task.sourceRecurringId != null) {
                recurringTaskDao.deleteWithId(task.sourceRecurringId)
            }
            taskDao.delete(task)
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val updatedTask = task.copy(isCompleted = !task.isCompleted)
            taskDao.update(updatedTask)
        }
    }

    fun getTaskById(id: Long): Task? {
        return _tasks.value.find { it.id == id }
    }
}