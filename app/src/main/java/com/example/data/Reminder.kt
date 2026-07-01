package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Priority {
    LOW, MEDIUM, HIGH
}

enum class Category {
    PERSONAL, WORK, SHOPPING, HEALTH, LIFE, OTHER
}

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val dueDate: Long? = null,
    val dueTime: String? = null,
    val isCompleted: Boolean = false,
    val priority: Priority = Priority.MEDIUM,
    val category: Category = Category.PERSONAL,
    val createdAt: Long = System.currentTimeMillis()
)
