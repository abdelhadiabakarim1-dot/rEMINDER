package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.ReminderDatabase
import com.example.data.ReminderRepository
import com.example.ui.ReminderAppContent
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ReminderViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val database = ReminderDatabase.getDatabase(applicationContext)
    val repository = ReminderRepository(database.reminderDao())
    val viewModel = ViewModelProvider(this, ReminderViewModel.Factory(repository))[ReminderViewModel::class.java]

    setContent {
      MyApplicationTheme {
        ReminderAppContent(viewModel = viewModel)
      }
    }
  }
}
