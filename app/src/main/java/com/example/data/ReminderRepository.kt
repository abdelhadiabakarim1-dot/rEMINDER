package com.example.data

import kotlinx.coroutines.flow.Flow

class ReminderRepository(private val reminderDao: ReminderDao) {
    val allReminders: Flow<List<Reminder>> = reminderDao.getAllReminders()

    suspend fun insert(reminder: Reminder) {
        reminderDao.insertReminder(reminder)
    }

    suspend fun deleteById(id: Int) {
        reminderDao.deleteReminderById(id)
    }

    suspend fun updateCompletion(id: Int, isCompleted: Boolean) {
        reminderDao.updateCompletionStatus(id, isCompleted)
    }

    suspend fun clearCompleted() {
        reminderDao.clearCompletedReminders()
    }
}
