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
import se.umu.c22jwg.gritly.data.Task
import se.umu.c22jwg.gritly.data.TaskDao

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase
        get() {
            if (db == null) {
                db = Room.databaseBuilder(
                    getApplication<Application>().applicationContext,
                    AppDatabase::class.java,
                    "gritly_database"
                ).build()
            }
            return db!!
        }

    companion object {
        private var db: AppDatabase? = null
    }

    private val taskDao: TaskDao = database.taskDao()

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
                // Sort tasks, incomplete first, then completed
                val sortedTasks = taskList.sortedWith(
                    compareBy<Task> { it.isCompleted }
                        .thenByDescending { it.id }
                )
                _tasks.value = sortedTasks
                _todayTasks.value = sortedTasks.filter { !it.isCompleted }
                _completedTasks.value = sortedTasks.filter { it.isCompleted }
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

    fun updateTask(task: Task) {
        viewModelScope.launch {
            taskDao.update(task)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
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