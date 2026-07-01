package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Category
import com.example.data.Priority
import com.example.data.Reminder
import com.example.data.ReminderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReminderViewModel(private val repository: ReminderRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedPriorityFilter = MutableStateFlow<Priority?>(null)
    val selectedPriorityFilter: StateFlow<Priority?> = _selectedPriorityFilter

    private val _selectedCategoryFilter = MutableStateFlow<Category?>(null)
    val selectedCategoryFilter: StateFlow<Category?> = _selectedCategoryFilter

    val reminders: StateFlow<List<Reminder>> = combine(
        repository.allReminders,
        _searchQuery,
        _selectedPriorityFilter,
        _selectedCategoryFilter
    ) { list, query, priority, category ->
        list.filter { reminder ->
            val matchesQuery = query.isEmpty() ||
                    reminder.title.contains(query, ignoreCase = true) ||
                    reminder.description.contains(query, ignoreCase = true)
            val matchesPriority = priority == null || reminder.priority == priority
            val matchesCategory = category == null || reminder.category == category
            matchesQuery && matchesPriority && matchesCategory
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setPriorityFilter(priority: Priority?) {
        _selectedPriorityFilter.value = priority
    }

    fun setCategoryFilter(category: Category?) {
        _selectedCategoryFilter.value = category
    }

    fun addReminder(
        title: String,
        description: String,
        dueDate: Long?,
        dueTime: String?,
        priority: Priority,
        category: Category
    ) {
        viewModelScope.launch {
            val newReminder = Reminder(
                title = title,
                description = description,
                dueDate = dueDate,
                dueTime = dueTime,
                priority = priority,
                category = category
            )
            repository.insert(newReminder)
        }
    }

    fun toggleReminderCompletion(reminder: Reminder) {
        viewModelScope.launch {
            repository.updateCompletion(reminder.id, !reminder.isCompleted)
        }
    }

    fun deleteReminder(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun clearCompleted() {
        viewModelScope.launch {
            repository.clearCompleted()
        }
    }

    class Factory(private val repository: ReminderRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ReminderViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ReminderViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
