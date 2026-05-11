package com.example.lab08

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TaskViewModel(private val dao: TaskDao) : ViewModel() {

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks

    private val _filter = MutableStateFlow("Todas")
    val filter: StateFlow<String> = _filter

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _sortAscending = MutableStateFlow(true)
    val sortAscending: StateFlow<Boolean> = _sortAscending

    private val _selectedCategory = MutableStateFlow("Todas")
    val selectedCategory: StateFlow<String> = _selectedCategory

    val categories = listOf("Todas", "General", "Trabajo", "Estudio", "Personal", "Salud")

    init {
        viewModelScope.launch {
            _tasks.value = dao.getAllTasks()
            FirestoreSync.syncAllTasks(_tasks.value)
        }
    }

    fun addTask(description: String, priority: Int = 3, category: String = "General", isRecurring: Boolean = false) {
        val newTask = Task(
            description = description,
            priority = priority,
            category = category,
            isRecurring = isRecurring
        )
        viewModelScope.launch {
            dao.insertTask(newTask)
            _tasks.value = dao.getAllTasks()
            val inserted = _tasks.value.last()
            FirestoreSync.uploadTask(inserted)
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val updatedTask = task.copy(isCompleted = !task.isCompleted)
            dao.updateTask(updatedTask)
            if (!task.isCompleted && task.isRecurring) {
                dao.insertTask(task.copy(id = 0, isCompleted = false))
            }
            _tasks.value = dao.getAllTasks()
            FirestoreSync.uploadTask(updatedTask)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            dao.deleteTask(task)
            _tasks.value = dao.getAllTasks()
            FirestoreSync.deleteTask(task)
        }
    }

    fun editTask(task: Task, newDescription: String, newPriority: Int, newCategory: String, newRecurring: Boolean) {
        viewModelScope.launch {
            val updatedTask = task.copy(
                description = newDescription,
                priority = newPriority,
                category = newCategory,
                isRecurring = newRecurring
            )
            dao.updateTask(updatedTask)
            _tasks.value = dao.getAllTasks()
            FirestoreSync.uploadTask(updatedTask)
        }
    }

    fun deleteAllTasks() {
        viewModelScope.launch {
            val current = _tasks.value
            dao.deleteAllTasks()
            _tasks.value = emptyList()
            current.forEach { FirestoreSync.deleteTask(it) }
        }
    }

    fun setFilter(filter: String) {
        _filter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleSort() {
        _sortAscending.value = !_sortAscending.value
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun getFilteredTasks(): List<Task> {
        var filtered = when (_filter.value) {
            "Pendientes" -> _tasks.value.filter { !it.isCompleted }
            "Completadas" -> _tasks.value.filter { it.isCompleted }
            else -> _tasks.value
        }

        if (_selectedCategory.value != "Todas") {
            filtered = filtered.filter { it.category == _selectedCategory.value }
        }

        val searched = if (_searchQuery.value.isEmpty()) filtered
        else filtered.filter { it.description.contains(_searchQuery.value, ignoreCase = true) }

        return if (_sortAscending.value) searched.sortedBy { it.priority }
        else searched.sortedByDescending { it.priority }
    }
}