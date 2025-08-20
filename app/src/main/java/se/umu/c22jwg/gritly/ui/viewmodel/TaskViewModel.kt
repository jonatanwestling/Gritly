package se.umu.c22jwg.gritly.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import se.umu.c22jwg.gritly.data.DailyEntryRepository
import se.umu.c22jwg.gritly.data.DatabaseProvider
import se.umu.c22jwg.gritly.data.RecurringTask
import se.umu.c22jwg.gritly.data.Task
import se.umu.c22jwg.gritly.data.TaskRepository
import java.time.LocalDate

/**
 * TaskViewModel is responsible for managing tasks and recurring tasks in the Gritly application.
 * This ViewModel handles the interaction with the model layer, providing a clean API for the UI to
 * access and modify task data. It uses Kotlin Coroutines and live data to handle asynchronous data
 * operations and state management.
 *
 * @param application The application context, used to initialize the Room database.
 */
class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val database = DatabaseProvider.getDatabase(application)

    /**
     * DAOs for accessing Task and RecurringTask data from the database.
     */
    private val taskRepository = TaskRepository(database.taskDao(), database.recurringTaskDao())

    /**
     * Initializes the ViewModel by loading all tasks, today's tasks, and recurring tasks.
     */
    init {
        loadAllTasks()
        loadTodayTasks()
        loadRecurringTasks()
    }

    // state for all tasks
    private val _allTasks = MutableLiveData<List<Task>>(emptyList())
    val allTasks: LiveData<List<Task>> = _allTasks

    // state for today's tasks
    private val _todayTasks = MutableLiveData<List<Task>>(emptyList())
    val todayTasks: LiveData<List<Task>> = _todayTasks

    // state for recurring tasks
    private val _recurringTasks = MutableLiveData<List<RecurringTask>>(emptyList())
    val recurringTasks: LiveData<List<RecurringTask>> = _recurringTasks


    /**
     * Loads all tasks from database and updates the general task list
     * Used for AddTaskScreen and general task management
     */
    private fun loadAllTasks() {
        viewModelScope.launch {
            val today = LocalDate.now().toString()
            taskRepository.getAllTasks.collect { taskList ->
                val sortedTasks = taskList.sortedWith(
                    compareBy<Task> { it.isCompleted }
                        .thenByDescending { it.id }
                )
                _allTasks.value = sortedTasks
                _todayTasks.value = sortedTasks.filter { it.date == today }
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



            taskRepository.getAllTasks.collect { taskList ->
                val todayTasks = taskList
                    .filter { it.date == today }
                    .sortedWith(
                        compareBy<Task> { it.isCompleted }
                            .thenByDescending { it.id }
                    )
                _todayTasks.value = todayTasks
            }
        }
    }

    /**
     * Loads recurring tasks from the database
     */
    private fun loadRecurringTasks() {
        viewModelScope.launch {
            taskRepository.recurringTasks.collect { recurringTaskList ->
                _recurringTasks.value = recurringTaskList
            }
        }
    }

    /**
     * Creates tasks from recurring tasks for the specified date
     */
    private suspend fun createTasksFromRecurringTasks(date: String) {
        val targetDate = LocalDate.parse(date)
        val dayOfWeek = targetDate.dayOfWeek.value
        // get the bit index for the day of the week
        val bitIndex = 7 - dayOfWeek

        taskRepository.recurringTasks.collect { recurringTasks ->
            for (recurring in recurringTasks) {
                // check if bit is set for the day
                val shouldRepeat = ((recurring.weekDays shr bitIndex) and 1) == 1
                // true if bit is set
                if (shouldRepeat) {
                    // make sure it does not already exist for that date
                    val existing = taskRepository.getTaskByRecurringSource(date, recurring.id)
                    if (existing == null) {
                        val newTask = Task(
                            title = recurring.title,
                            date = date,
                            isCompleted = false,
                            sourceRecurringId = recurring.id
                        )
                        taskRepository.insertTask(newTask)
                    }
                }
            }
        }
    }

    /**
     * Adds a new normal task to the database
     *
     * @param title The title of the task
     * @param date The date for the task in "yyyy-MM-dd" format
     * @param description (currently not being used)
     */
    fun addTask(title: String, date: String, description: String? = null) {
        viewModelScope.launch {
            val task = Task(
                title = title,
                date = date,
                description = description,
                isCompleted = false
            )
            taskRepository.insertTask(task)
        }
    }

    /**
     * Adds a new recurring task to the database
     *
     * @param title The title of the recurring task
     * @param days A list of booleans representing the days of the week (Monday to Sunday)
     */
    fun addRecurringTask(title: String, days: List<Boolean>) {
        viewModelScope.launch {
            // convert the list of booleans to bitmask
            var bitmask = 0
            for (day in days) {
                // set the bit if true
                bitmask = (bitmask shl 1) or if (day) 1 else 0
            }
            // create the task
            val task = RecurringTask(
                title = title,
                weekDays = bitmask
            )
            taskRepository.insertRecurringTask(task)
        }
    }


    /**
     * Deletes a task and its associated recurring task if applicable
     * @param task the task to be deleted
     */
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            if (task.sourceRecurringId != null) {
                taskRepository.deleteRecurringTaskById(task.sourceRecurringId)
            }
            taskRepository.deleteTask(task)
        }
    }

    /**
     * Deletes a recurring task
     *
     * @param id the id of the reccuring task to be deleted
     */
    fun deleteTaskFromId(id: Long) {
        viewModelScope.launch {
            taskRepository.deleteRecurringTaskById(id);
        }
    }

    /**
     * Toggles the completion status of a task
     *
     * @param task the task to be toggled
     */
    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val updatedTask = task.copy(isCompleted = !task.isCompleted)
            taskRepository.updateTask(updatedTask)
        }
    }

    /**
     * Gets recurring tasks that are planned for a specific date
     *
     * @param date the date for the tasks
     * @param recurringTasks a list of recurring tasks to check
     * @return A list of recurring tasks that is recurring the day specified by the date param
     */
    fun getRecurringTasksForDate(
        date: String,
        recurringTasks: List<RecurringTask>
    ): List<RecurringTask> {
        val targetDate = LocalDate.parse(date)
        val dayOfWeek = targetDate.dayOfWeek.value
        val bitIndex = 7 - dayOfWeek
        return recurringTasks.filter { ((it.weekDays shr bitIndex) and 1) == 1 }
    }
}