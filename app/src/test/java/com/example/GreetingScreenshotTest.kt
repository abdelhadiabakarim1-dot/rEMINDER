package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.Category
import com.example.data.Priority
import com.example.data.Reminder
import com.example.ui.ReminderCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val reminder = Reminder(
      id = 1,
      title = "Prepare Project Presentation",
      description = "Review slides and practice speech before the team meeting.",
      priority = Priority.HIGH,
      category = Category.WORK,
      dueDate = 1782916326000L,
      dueTime = "10:30"
    )
    composeTestRule.setContent {
      MyApplicationTheme {
        ReminderCard(
          reminder = reminder,
          onToggleComplete = {},
          onDelete = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
