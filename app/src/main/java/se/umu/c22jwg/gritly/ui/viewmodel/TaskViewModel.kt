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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import se.umu.c22jwg.gritly.data.AppDatabase
import se.umu.c22jwg.gritly.data.DailyEntry
import se.umu.c22jwg.gritly.data.DailyEntryDao
import se.umu.c22jwg.gritly.data.DailyEntryRepository
import se.umu.c22jwg.gritly.data.DatabaseProvider
import se.umu.c22jwg.gritly.data.RecurringTask
import se.umu.c22jwg.gritly.data.RecurringTaskDao
import se.umu.c22jwg.gritly.data.Task
import se.umu.c22jwg.gritly.data.TaskDao
import se.umu.c22jwg.gritly.data.TaskRepository
import java.time.LocalDate

/**
 * ViewModel for managing tasks and recurring tasks in the Gritly application.
 * This ViewModel handles the interaction with the Room database, providing a clean API for the UI to access and modify task data.
 * It uses Kotlin Coroutines and Flow to handle asynchronous data operations and state management.
 * 
 * @param application The application context, used to initialize the Room database.
 * 
 * This ViewModel is responsible for:
 * - Loading and managing tasks and recurring tasks from the database.
 * - Providing state flows for all tasks, today's tasks, completed tasks, and recurring tasks.
 * - Allowing the UI to set a selected date for planning tasks.
 * - Adding, updating, and deleting tasks and recurring tasks.
 * - Creating tasks from recurring tasks for a specific date.
 * - Toggling task completion status.
 * - Filtering tasks based on the selected date.
 */
class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val database = DatabaseProvider.getDatabase(application)
    /**
     * DAOs for accessing Task and RecurringTask data from the database.
     */
    private val taskRepository = TaskRepository(database.taskDao(), database.recurringTaskDao())
    private val dailyEntryRepository = DailyEntryRepository(database.dailyEntryDao())


    /**
     * Constructor that loads the initials data.
     */
    init {
        loadAllTasks()
        loadTodayTasks()
        loadRecurringTasks()
    }

    // state for all tasks
    private val _allTasks = MutableStateFlow<List<Task>>(emptyList())
    val allTasks: StateFlow<List<Task>> = _allTasks.asStateFlow()

    // state for today's tasks 
    private val _todayTasks = MutableStateFlow<List<Task>>(emptyList())
    val todayTasks: StateFlow<List<Task>> = _todayTasks.asStateFlow()

    // state for completed tasks 
    private val _completedTasks = MutableStateFlow<List<Task>>(emptyList())
    val completedTasks: StateFlow<List<Task>> = _completedTasks.asStateFlow()

    // state for recurring tasks
    private val _recurringTasks = MutableStateFlow<List<RecurringTask>>(emptyList())
    val recurringTasks: StateFlow<List<RecurringTask>> = _recurringTasks.asStateFlow()

    // state for selected date, defaulting to tomorrow (the user can only plan tasks for tomorrow or later)
    private val _selectedDate = MutableStateFlow(LocalDate.now().plusDays(1).toString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    /**
     * Sets the selected date for planning tasks.
     * This is used to filter tasks based on the selected date
     * @param date The date to set as selected, in "yyyy-MM-dd" format.
     */
    fun setSelectedDate(date: String) {
        _selectedDate.value = date
    }

    /**
     * Combines all tasks and recurring tasks to create a list of planned tasks for the selected date.
     * This list includes both regular tasks and recurring tasks that are scheduled for the selected date.
     * The list updates automatically when the selected date, allTasks, or recurringTasks change.
     * 
     * This is used to display tasks in the planning screen.
     * @return A Flow that emits a list of pairs, where each pair contains a Task and a RecurringTask.
     *         The Task is null if the recurring task is planned for that date or the other way around.
     */
    val plannedTasks = combine(allTasks, recurringTasks, selectedDate) { allTasks, recurringTasks, selectedDate ->
        val plannedTasks = allTasks.filter { it.date == selectedDate }
        // Filter out recurring tasks that are already created as regular tasks for the selected date
        val recurringTaskIdsInTasks = plannedTasks.mapNotNull { it.sourceRecurringId }.toSet()
        val plannedRecurringTasks = getRecurringTasksForDate(selectedDate, recurringTasks)
            .filter { it.id !in recurringTaskIdsInTasks }
        // Combine both lists into a list of pairs
        plannedTasks.map { it to null } + plannedRecurringTasks.map { null to it }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
                _completedTasks.value = sortedTasks.filter { it.isCompleted }
                _todayTasks.value = sortedTasks.filter { it.date ==  today}
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
            
            // create normal tasks from recurring tasks so they can be marked as completed
            // and behaves as a normal task.
            createTasksFromRecurringTasks(today)

            // Flow ist for function
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
     * Refreshes today's tasks
     */
    fun refreshTodayTasks() {
        loadTodayTasks()
    }

    /**
     * Refreshes all tasks
     */
    fun refreshAllTasks() {
        loadAllTasks()
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
     * Updates an existing task
     */
    fun updateTask(task: Task) {
        viewModelScope.launch {
            taskRepository.updateTask(task)
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
    fun deleteTaskFromId(id: Long){
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
     * Gets a specific task by its ID
     *
     * @param id the id of the task
     * @return Task if there is any found
     */
    fun getTaskById(id: Long): Task? {
        return _allTasks.value.find { it.id == id }
    }

    /**
     * Gets tasks for a specific date
     *
     * @param date a specific date to get all tasks from
     * @return A list of tasks that are planned on that date
     */
    fun getTasksForDate(date: String): List<Task> {
        return _allTasks.value.filter { it.date == date }
    }

    /**
     * Gets recurring tasks that are planned for a specific date
     *
     * @param date the date for the tasks
     * @param recurringTasks a list of recurring tasks to check
     * @return A list of recurring tasks that is recurring the day specified by the date param
     */
    fun getRecurringTasksForDate(date: String, recurringTasks: List<RecurringTask>): List<RecurringTask> {
        val targetDate = LocalDate.parse(date)
        val dayOfWeek = targetDate.dayOfWeek.value // 1 = Monday, ..., 7 = Sunday
        val bitIndex = 7 - dayOfWeek // Map Monday=6, Sunday=0
        return recurringTasks.filter { ((it.weekDays shr bitIndex) and 1) == 1 }
    }

    fun addPhotoPath(path: String) {
        viewModelScope.launch {

            dailyEntryRepository.insertEntry( DailyEntry(
                date = LocalDate.now().toString(),
                note = "testo",
                path

            ))
        }

    }
}
