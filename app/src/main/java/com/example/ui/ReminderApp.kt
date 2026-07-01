package com.example.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.Category
import com.example.data.Priority
import com.example.data.Reminder
import com.example.viewmodel.ReminderViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderAppContent(
    viewModel: ReminderViewModel,
    modifier: Modifier = Modifier
) {
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedPriority by viewModel.selectedPriorityFilter.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }

    // Derive simple stats
    val totalCount = reminders.size
    val completedCount = reminders.count { it.isCompleted }
    val pendingCount = totalCount - completedCount
    val completionPercentage = if (totalCount > 0) (completedCount.toFloat() / totalCount.toFloat()) else 0.0f

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("add_reminder_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Reminder",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 1. Dashboard Header Section (Greeting & Stats)
            DashboardHeader(
                pendingCount = pendingCount,
                completedCount = completedCount,
                progress = completionPercentage,
                onClearCompleted = { viewModel.clearCompleted() }
            )

            // 2. Search Bar
            SearchBarSection(
                query = searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) }
            )

            // 3. Multi-Filters Section (Priority & Category)
            FilterChipsSection(
                selectedPriority = selectedPriority,
                onPrioritySelect = { viewModel.setPriorityFilter(it) },
                selectedCategory = selectedCategory,
                onCategorySelect = { viewModel.setCategoryFilter(it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Reminder List / Empty State
            if (reminders.isEmpty()) {
                EmptyStateSection(
                    hasFilters = searchQuery.isNotEmpty() || selectedPriority != null || selectedCategory != null,
                    onResetFilters = {
                        viewModel.setSearchQuery("")
                        viewModel.setPriorityFilter(null)
                        viewModel.setCategoryFilter(null)
                    },
                    onAddClick = { showAddDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 88.dp, start = 16.dp, end = 16.dp, top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(reminders, key = { it.id }) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            onToggleComplete = { viewModel.toggleReminderCompletion(reminder) },
                            onDelete = { viewModel.deleteReminder(reminder.id) }
                        )
                    }
                }
            }
        }

        // Add Reminder dialog sheet
        if (showAddDialog) {
            AddReminderDialog(
                onDismiss = { showAddDialog = false },
                onSave = { title, desc, date, time, priority, category ->
                    viewModel.addReminder(title, desc, date, time, priority, category)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun DashboardHeader(
    pendingCount: Int,
    completedCount: Int,
    progress: Float,
    onClearCompleted: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Your Reminders",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Stay organized, stay fast.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (completedCount > 0) {
                    IconButton(
                        onClick = onClearCompleted,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear completed",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = "Pending",
                    count = pendingCount,
                    imageVector = Icons.Default.PendingActions,
                    color = MaterialTheme.colorScheme.primary
                )
                StatItem(
                    label = "Completed",
                    count = completedCount,
                    imageVector = Icons.Default.TaskAlt,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Percentage completion bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun StatItem(
    label: String,
    count: Int,
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SearchBarSection(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search your reminders...") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search"
                    )
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("search_bar")
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChipsSection(
    selectedPriority: Priority?,
    onPrioritySelect: (Priority?) -> Unit,
    selectedCategory: Category?,
    onCategorySelect: (Category?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        // Priority Filter Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedPriority == null,
                    onClick = { onPrioritySelect(null) },
                    label = { Text("All Priorities") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
            items(Priority.values()) { priority ->
                val color = when (priority) {
                    Priority.HIGH -> Color(0xFFE57373)
                    Priority.MEDIUM -> Color(0xFFFFB74D)
                    Priority.LOW -> Color(0xFF81C784)
                }
                FilterChip(
                    selected = selectedPriority == priority,
                    onClick = { onPrioritySelect(priority) },
                    label = { Text(priority.name) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Category Filter Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onCategorySelect(null) },
                    label = { Text("All Categories") }
                )
            }
            items(Category.values()) { category ->
                val icon = when (category) {
                    Category.PERSONAL -> Icons.Default.Person
                    Category.WORK -> Icons.Default.Work
                    Category.SHOPPING -> Icons.Default.ShoppingCart
                    Category.HEALTH -> Icons.Default.Favorite
                    Category.LIFE -> Icons.Default.Home
                    Category.OTHER -> Icons.Default.Category
                }
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategorySelect(category) },
                    label = { Text(category.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    leadingIcon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun ReminderCard(
    reminder: Reminder,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val priorityColor = when (reminder.priority) {
        Priority.HIGH -> Color(0xFFE57373)
        Priority.MEDIUM -> Color(0xFFFFB74D)
        Priority.LOW -> Color(0xFF81C784)
    }

    val categoryIcon = when (reminder.category) {
        Category.PERSONAL -> Icons.Default.Person
        Category.WORK -> Icons.Default.Work
        Category.SHOPPING -> Icons.Default.ShoppingCart
        Category.HEALTH -> Icons.Default.Favorite
        Category.LIFE -> Icons.Default.Home
        Category.OTHER -> Icons.Default.Category
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reminder_item_card_${reminder.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (reminder.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (reminder.isCompleted) 0.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Priority stripe on the left
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(priorityColor)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive Checkbox
                IconButton(
                    onClick = onToggleComplete,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("complete_checkbox_${reminder.id}")
                ) {
                    Icon(
                        imageVector = if (reminder.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = "Toggle Complete",
                        tint = if (reminder.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Content area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        fontWeight = FontWeight.Bold,
                        color = if (reminder.isCompleted) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (reminder.description.isNotEmpty()) {
                        Text(
                            text = reminder.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = if (reminder.isCompleted) 0.5f else 0.8f
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Badges row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Category Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = categoryIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = reminder.category.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        // Date/Time Badge (if exists)
                        if (reminder.dueDate != null) {
                            val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            val dateStr = formatter.format(Date(reminder.dueDate))
                            val displayTime = reminder.dueTime?.let { " at $it" } ?: ""

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Event,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$dateStr$displayTime",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                // Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("delete_reminder_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Reminder",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateSection(
    hasFilters: Boolean,
    onResetFilters: () -> Unit,
    onAddClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .wrapContentHeight(Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // High quality generated illustration
        Image(
            painter = painterResource(id = R.drawable.ic_reminder_hero),
            contentDescription = "No reminders",
            modifier = Modifier
                .size(220.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (hasFilters) "No Matching Reminders" else "Capture Your Thoughts",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (hasFilters) {
                "We couldn't find any reminders matching your search or filters."
            } else {
                "Quickly add and persistent work items, groceries, health tasks, or custom notes."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (hasFilters) {
            Button(
                onClick = onResetFilters,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterAltOff,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear All Filters")
            }
        } else {
            Button(
                onClick = onAddClick,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create a Reminder")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderDialog(
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String,
        dueDate: Long?,
        dueTime: String?,
        priority: Priority,
        category: Category
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(Priority.MEDIUM) }
    var selectedCategory by remember { mutableStateOf(Category.PERSONAL) }

    var dueDate by remember { mutableStateOf<Long?>(null) }
    var dueTime by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(title.trim(), description.trim(), dueDate, dueTime, selectedPriority, selectedCategory)
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("save_reminder_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = {
            Text(
                text = "New Reminder",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title Field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title *") },
                    placeholder = { Text("What needs to be done?") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_title_input")
                )

                // Description Field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Details (Optional)") },
                    placeholder = { Text("Add more information...") },
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_desc_input")
                )

                // Priority Selection
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Priority",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Priority.values().forEach { priority ->
                            val isSelected = selectedPriority == priority
                            val color = when (priority) {
                                Priority.HIGH -> Color(0xFFE57373)
                                Priority.MEDIUM -> Color(0xFFFFB74D)
                                Priority.LOW -> Color(0xFF81C784)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) color.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(
                                            alpha = 0.4f
                                        )
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        color = if (isSelected) color else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedPriority = priority }
                                    .padding(vertical = 10.dp)
                                    .testTag("priority_chip_${priority.name.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = priority.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Category Selection
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val categories = Category.values()
                        val rows = categories.toList().chunked(3)
                        rows.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { category ->
                                    val isSelected = selectedCategory == category
                                    val icon = when (category) {
                                        Category.PERSONAL -> Icons.Default.Person
                                        Category.WORK -> Icons.Default.Work
                                        Category.SHOPPING -> Icons.Default.ShoppingCart
                                        Category.HEALTH -> Icons.Default.Favorite
                                        Category.LIFE -> Icons.Default.Home
                                        Category.OTHER -> Icons.Default.Category
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                            )
                                            .border(
                                                width = 1.5.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable { selectedCategory = category }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = category.name.lowercase().replaceFirstChar { it.uppercase() },
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Date & Time Picker Triggers
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Schedule",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Date picker button
                        OutlinedButton(
                            onClick = {
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        val selectedCal = Calendar.getInstance()
                                        selectedCal.set(year, month, day, 0, 0, 0)
                                        dueDate = selectedCal.timeInMillis
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = dueDate?.let { dateFormatter.format(Date(it)) } ?: "Pick Date",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Time picker button
                        OutlinedButton(
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        dueTime = String.format("%02d:%02d", hour, minute)
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    true
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = dueTime ?: "Pick Time",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Clear schedule button
                    if (dueDate != null || dueTime != null) {
                        TextButton(
                            onClick = {
                                dueDate = null
                                dueTime = null
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear Schedule")
                        }
                    }
                }
            }
        }
    )
}
