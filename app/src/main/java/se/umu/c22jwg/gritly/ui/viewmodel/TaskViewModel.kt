package se.umu.c22jwg.gritly.ui.viewmodel

import android.app.Application
import android.util.Log
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
        loadAllTasks()
        loadTodayTasks()
        loadRecurringTasks()
    }

    // State for all tasks (for AddTaskScreen and general list)
    private val _allTasks = MutableStateFlow<List<Task>>(emptyList())
    val allTasks: StateFlow<List<Task>> = _allTasks.asStateFlow()

    // State for today's tasks (for TodayScreen)
    private val _todayTasks = MutableStateFlow<List<Task>>(emptyList())
    val todayTasks: StateFlow<List<Task>> = _todayTasks.asStateFlow()

    // State for completed tasks (for HistoryScreen)
    private val _completedTasks = MutableStateFlow<List<Task>>(emptyList())
    val completedTasks: StateFlow<List<Task>> = _completedTasks.asStateFlow()

    // State for recurring tasks
    private val _recurringTasks = MutableStateFlow<List<RecurringTask>>(emptyList())
    val recurringTasks: StateFlow<List<RecurringTask>> = _recurringTasks.asStateFlow()

    /**
     * Loads all tasks from database and updates the general task list
     * Used for AddTaskScreen and general task management
     */
    private fun loadAllTasks() {
        viewModelScope.launch {
            taskDao.getAllTasks().collect { taskList ->
                val sortedTasks = taskList.sortedWith(
                    compareBy<Task> { it.isCompleted }
                        .thenByDescending { it.id }
                )
                _allTasks.value = sortedTasks
                _completedTasks.value = sortedTasks.filter { it.isCompleted }
            }
        }
    }

    /**
     * Loads and manages today's tasks, including creating tasks from recurring tasks
     * Used specifically for TodayScreen
     */
    private fun loadTodayTasks() {
        viewModelScope.launch {
            val today = LocalDate.now().toString()
            
            // First, create tasks from recurring tasks for today
            createTasksFromRecurringTasks(today)
            
            // Then load today's tasks
            taskDao.getAllTasks().collect { taskList ->
                val todayTasks = taskList.filter { 
                    it.date == today && !it.isCompleted 
                }.sortedByDescending { it.id }
                _todayTasks.value = todayTasks
            }
        }
    }

    /**
     * Loads recurring tasks from the database
     */
    private fun loadRecurringTasks() {
        viewModelScope.launch {
            recurringTaskDao.getRecurringTasks().collect { recurringTaskList ->
                _recurringTasks.value = recurringTaskList
            }
        }
    }

    /**
     * Creates tasks from recurring tasks for the specified date
     */
    private suspend fun createTasksFromRecurringTasks(date: String) {
        val targetDate = LocalDate.parse(date)
        val dayOfWeek = targetDate.dayOfWeek.value // 1 = Monday, ..., 7 = Sunday

        val bitIndex = 7 - dayOfWeek // Map Monday=6, Sunday=0

        recurringTaskDao.getRecurringTasks().collect { recurringTasks ->
            for (recurring in recurringTasks) {
                val shouldRepeat = ((recurring.weekDays shr bitIndex) and 1) == 1

                if (shouldRepeat) {
                    val existing = taskDao.getTaskByRecurringSource(date, recurring.id)
                    if (existing == null) {
                        val newTask = Task(
                            title = recurring.title,
                            date = date,
                            isCompleted = false,
                            sourceRecurringId = recurring.id
                        )
                        taskDao.insert(newTask)
                    }
                }
            }
        }
    }

    

    /**
     * Refreshes today's tasks (useful when returning to TodayScreen)
     */
    fun refreshTodayTasks() {
        loadTodayTasks()
    }

    /**
     * Refreshes all tasks (useful when returning to AddTaskScreen)
     */
    fun refreshAllTasks() {
        loadAllTasks()
    }

    /**
     * Adds a new task to the database
     */
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

    /**
     * Adds a new recurring task to the database
     */
    fun addRecurringTask(title: String, days: List<Boolean>) {
        viewModelScope.launch {
            var bitmask = 0
            for (day in days) {
                bitmask = (bitmask shl 1) or if (day) 1 else 0
            }
            val task = RecurringTask(
                title = title,
                weekDays = bitmask
            )
            recurringTaskDao.insert(task)
        }
    }

    /**
     * Updates an existing task
     */
    fun updateTask(task: Task) {
        viewModelScope.launch {
            taskDao.update(task)
        }
    }

    /**
     * Deletes a task and its associated recurring task if applicable
     */
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            if (task.sourceRecurringId != null) {
                recurringTaskDao.deleteWithId(task.sourceRecurringId)
            }
            taskDao.delete(task)
        }
    }

    fun deleteTaskFromId(id: Long){
        viewModelScope.launch {
            recurringTaskDao.deleteWithId(id);
        }
    }

    /**
     * Toggles the completion status of a task
     */
    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val updatedTask = task.copy(isCompleted = !task.isCompleted)
            taskDao.update(updatedTask)
        }
    }

    /**
     * Gets a specific task by its ID
     */
    fun getTaskById(id: Long): Task? {
        return _allTasks.value.find { it.id == id }
    }

    /**
     * Gets tasks for a specific date
     */
    fun getTasksForDate(date: String): List<Task> {
        return _allTasks.value.filter { it.date == date }
    }

    /**
     * Ensures tasks are created for a specific date from recurring tasks
     * Call this when viewing a specific date to ensure recurring tasks are available
     */
    fun ensureTasksForDate(date: String) {
        viewModelScope.launch {
            createTasksFromRecurringTasks(date)
        }
    }

    /**
     * Gets recurring tasks that are planned for a specific date
     * This does NOT create new Task entries, just returns which recurring tasks are scheduled
     */
    fun getRecurringTasksForDate(date: String): List<RecurringTask> {
        val targetDate = LocalDate.parse(date)
        val dayOfWeek = targetDate.dayOfWeek.value // 1 = Monday, ..., 7 = Sunday
        val bitIndex = 7 - dayOfWeek // Map Monday=6, Sunday=0
        
        return _recurringTasks.value.filter { recurringTask ->
            ((recurringTask.weekDays shr bitIndex) and 1) == 1
        }
    }
}