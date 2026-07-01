import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:intl/intl.dart';
import 'models/reminder.dart';

void main() {
  runApp(const QuickReminderApp());
}

class QuickReminderApp extends StatelessWidget {
  const QuickReminderApp({Key? key}) : super(key: key);

  @override
  Widget build(key) {
    return MaterialApp(
      title: 'Quick Reminder',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: const ColorScheme(
          brightness: Brightness.light,
          primary: Color(0xFF6750A4),
          onPrimary: Colors.white,
          primaryContainer: Color(0xFFEADDFF),
          onPrimaryContainer: Color(0xFF21005D),
          secondary: Color(0xFF625B71),
          onSecondary: Colors.white,
          secondaryContainer: Color(0xFFE8DEF8),
          onSecondaryContainer: Color(0xFF1D192B),
          tertiary: Color(0xFF7D5260),
          onTertiary: Colors.white,
          background: Color(0xFFFEF7FF),
          onBackground: Color(0xFF1D1B20),
          surface: Color(0xFFFEF7FF),
          onSurface: Color(0xFF1D1B20),
          surfaceVariant: Color(0xFFF3EDF7),
          onSurfaceVariant: Color(0xFF49454F),
          outline: Color(0xFFCAC4D0),
          outlineVariant: Color(0xFFE7E0EC),
          error: Color(0xFFB3261E),
          onError: Colors.white,
        ),
        fontFamily: 'sans-serif',
      ),
      home: const ReminderHomeScreen(),
    );
  }
}

class ReminderHomeScreen extends StatefulWidget {
  const ReminderHomeScreen({Key? key}) : super(key: key);

  @override
  State<ReminderHomeScreen> createState() => _ReminderHomeScreenState();
}

class _ReminderHomeScreenState extends State<ReminderHomeScreen> {
  List<Reminder> _allReminders = [];
  List<Reminder> _filteredReminders = [];

  String _searchQuery = '';
  Priority? _selectedPriorityFilter;
  Category? _selectedCategoryFilter;

  @override
  void initState() {
    super.initState();
    _loadReminders();
  }

  Future<void> _loadReminders() async {
    final prefs = await SharedPreferences.getInstance();
    final List<String>? reminderStrings = prefs.getStringList('reminders');
    if (reminderStrings != null) {
      setState(() {
        _allReminders = reminderStrings
            .map((s) => Reminder.fromJson(s))
            .toList();
        _applyFilters();
      });
    } else {
      // Load seed reminders if empty
      _allReminders = [
        Reminder(
          id: 1,
          title: 'Prepare Project Presentation',
          description: 'Review slides and practice speech before the team meeting.',
          priority: Priority.HIGH,
          category: Category.WORK,
          dueDate: DateTime.now().add(const Duration(days: 1)),
          dueTime: '10:30',
        ),
        Reminder(
          id: 2,
          title: 'Take Daily Vitamins',
          description: 'Take with food after breakfast.',
          priority: Priority.MEDIUM,
          category: Category.HEALTH,
          dueTime: '08:00',
        ),
        Reminder(
          id: 3,
          title: 'Buy Fresh Groceries',
          description: 'Get apples, milk, eggs, and bread.',
          priority: Priority.LOW,
          category: Category.SHOPPING,
          isCompleted: true,
        ),
      ];
      await _saveReminders();
    }
  }

  Future<void> _saveReminders() async {
    final prefs = await SharedPreferences.getInstance();
    final reminderStrings = _allReminders.map((r) => r.toJson()).toList();
    await prefs.setStringList('reminders', reminderStrings);
    _applyFilters();
  }

  void _applyFilters() {
    setState(() {
      _filteredReminders = _allReminders.where((reminder) {
        final matchesQuery = reminder.title.toLowerCase().contains(_searchQuery.toLowerCase()) ||
            reminder.description.toLowerCase().contains(_searchQuery.toLowerCase());
        final matchesPriority = _selectedPriorityFilter == null || reminder.priority == _selectedPriorityFilter;
        final matchesCategory = _selectedCategoryFilter == null || reminder.category == _selectedCategoryFilter;
        return matchesQuery && matchesPriority && matchesCategory;
      }).toList();
    });
  }

  void _addReminder(Reminder reminder) {
    setState(() {
      _allReminders.insert(0, reminder);
      _saveReminders();
    });
  }

  void _toggleComplete(int id) {
    setState(() {
      final index = _allReminders.indexWhere((r) => r.id == id);
      if (index != -1) {
        _allReminders[index] = _allReminders[index].copyWith(
          isCompleted: !_allReminders[index].isCompleted,
        );
        _saveReminders();
      }
    });
  }

  void _deleteReminder(int id) {
    setState(() {
      _allReminders.removeWhere((r) => r.id == id);
      _saveReminders();
    });
  }

  void _clearCompleted() {
    setState(() {
      _allReminders.removeWhere((r) => r.isCompleted);
      _saveReminders();
    });
  }

  @override
  Widget build(BuildContext context) {
    final totalCount = _allReminders.length;
    final completedCount = _allReminders.where((r) => r.isCompleted).length;
    final pendingCount = totalCount - completedCount;
    final progress = totalCount > 0 ? (completedCount / totalCount) : 0.0;

    return Scaffold(
      backgroundColor: Theme.of(context).colorScheme.background,
      appBar: AppBar(
        title: const Text(
          'Reminders',
          style: TextStyle(fontWeight: FontWeight.w600, fontSize: 24),
        ),
        centerTitle: false,
        backgroundColor: Colors.transparent,
        elevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.info_outline),
            onPressed: () {
              showAboutDialog(
                context: context,
                applicationName: 'Quick Reminder',
                applicationVersion: '1.0.0',
                applicationIcon: const Icon(Icons.check_circle_outline, size: 48),
                children: [
                  const Text('A native look & feel Flutter application with professional Material 3 styling.'),
                ],
              );
            },
          ),
        ],
      ),
      body: Column(
        children: [
          // 1. Dashboard Stat Header
          _buildDashboardHeader(pendingCount, completedCount, progress),

          // 2. Search Bar
          _buildSearchBar(),

          // 3. Multi-Filters (Priority & Category horizontal lists)
          _buildFiltersSection(),

          const SizedBox(height: 12),

          // 4. Reminders List View or Empty State
          Expanded(
            child: _filteredReminders.isEmpty
                ? _buildEmptyState()
                : ListView.builder(
                    padding: const EdgeInsets.only(left: 16, right: 16, bottom: 88, top: 4),
                    itemCount: _filteredReminders.length,
                    itemBuilder: (context, index) {
                      final reminder = _filteredReminders[index];
                      return _buildReminderCard(reminder);
                    },
                  ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.large(
        onPressed: _showAddReminderDialog,
        backgroundColor: Theme.of(context).colorScheme.primaryContainer,
        foregroundColor: Theme.of(context).colorScheme.onPrimaryContainer,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
        child: const Icon(Icons.add, size: 32),
      ),
    );
  }

  Widget _buildDashboardHeader(int pendingCount, int completedCount, double progress) {
    final theme = Theme.of(context);
    return Card(
      margin: const EdgeInsets.all(16),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
      color: theme.colorScheme.surfaceVariant,
      elevation: 0,
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Your Status',
                      style: theme.textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.bold,
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      'Stay organized, stay fast.',
                      style: theme.textTheme.bodyMedium?.copyWith(
                        color: theme.colorScheme.onSurfaceVariant.withOpacity(0.8),
                      ),
                    ),
                  ],
                ),
                if (completedCount > 0)
                  IconButton(
                    icon: Icon(Icons.delete_sweep, color: theme.colorScheme.error),
                    style: IconButton.styleFrom(
                      backgroundColor: theme.colorScheme.error.withOpacity(0.1),
                    ),
                    onPressed: _clearCompleted,
                    tooltip: 'Clear completed',
                  ),
              ],
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: _buildStatItem(
                    label: 'Pending',
                    count: pendingCount,
                    icon: Icons.pending_actions,
                    color: theme.colorScheme.primary,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: _buildStatItem(
                    label: 'Completed',
                    count: completedCount,
                    icon: Icons.task_alt,
                    color: theme.colorScheme.tertiary,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(4),
                    child: LinearProgressIndicator(
                      value: progress,
                      minHeight: 8,
                      backgroundColor: theme.colorScheme.background,
                      valueColor: AlwaysStoppedAnimation<Color>(theme.colorScheme.primary),
                    ),
                  ),
                ),
                const SizedBox(width: 12),
                Text(
                  '${(progress * 100).toInt()}%',
                  style: theme.textTheme.labelLarge?.copyWith(
                    fontWeight: FontWeight.bold,
                    color: theme.colorScheme.primary,
                  ),
                ),
              ],
            )
          ],
        ),
      ),
    );
  }

  Widget _buildStatItem({
    required String label,
    required int count,
    required IconData icon,
    required Color color,
  }) {
    final theme = Theme.of(context);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: color.withOpacity(0.08),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Row(
        children: [
          Icon(icon, color: color, size: 24),
          const SizedBox(width: 12),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                '$count',
                style: theme.textTheme.titleMedium?.copyWith(
                  fontWeight: FontWeight.bold,
                  color: color,
                ),
              ),
              Text(
                label,
                style: theme.textTheme.labelSmall?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant.withOpacity(0.7),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildSearchBar() {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: SearchBar(
        hintText: 'Search your reminders...',
        elevation: const MaterialStatePropertyAll(0),
        backgroundColor: MaterialStatePropertyAll(theme.colorScheme.surfaceVariant.withOpacity(0.5)),
        leading: Icon(Icons.search, color: theme.colorScheme.onSurfaceVariant),
        onChanged: (val) {
          setState(() {
            _searchQuery = val;
            _applyFilters();
          });
        },
        trailing: _searchQuery.isNotEmpty
            ? [
                IconButton(
                  icon: const Icon(Icons.clear),
                  onPressed: () {
                    setState(() {
                      _searchQuery = '';
                      _applyFilters();
                    });
                  },
                )
              ]
            : null,
      ),
    );
  }

  Widget _buildFiltersSection() {
    final theme = Theme.of(context);
    return Column(
      children: [
        const SizedBox(height: 12),
        // Priority horizontal list
        SizedBox(
          height: 40,
          child: ListView(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: 16),
            children: [
              ChoiceChip(
                label: const Text('All Priorities'),
                selected: _selectedPriorityFilter == null,
                onSelected: (selected) {
                  if (selected) {
                    setState(() {
                      _selectedPriorityFilter = null;
                      _applyFilters();
                    });
                  }
                },
              ),
              ...Priority.values.map((priority) {
                final isSelected = _selectedPriorityFilter == priority;
                final priorityColor = _getPriorityColor(priority);
                return Padding(
                  padding: const EdgeInsets.only(left: 8),
                  child: ChoiceChip(
                    label: Text(priority.name),
                    selected: isSelected,
                    avatar: CircleAvatar(
                      backgroundColor: priorityColor,
                      radius: 4,
                    ),
                    onSelected: (selected) {
                      setState(() {
                        _selectedPriorityFilter = selected ? priority : null;
                        _applyFilters();
                      });
                    },
                  ),
                );
              }).toList(),
            ],
          ),
        ),
        const SizedBox(height: 8),
        // Category horizontal list
        SizedBox(
          height: 40,
          child: ListView(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: 16),
            children: [
              ChoiceChip(
                label: const Text('All Categories'),
                selected: _selectedCategoryFilter == null,
                onSelected: (selected) {
                  if (selected) {
                    setState(() {
                      _selectedCategoryFilter = null;
                      _applyFilters();
                    });
                  }
                },
              ),
              ...Category.values.map((category) {
                final isSelected = _selectedCategoryFilter == category;
                return Padding(
                  padding: const EdgeInsets.only(left: 8),
                  child: ChoiceChip(
                    label: Text(_formatEnumName(category.name)),
                    selected: isSelected,
                    avatar: Icon(
                      _getCategoryIcon(category),
                      size: 16,
                    ),
                    onSelected: (selected) {
                      setState(() {
                        _selectedCategoryFilter = selected ? category : null;
                        _applyFilters();
                      });
                    },
                  ),
                );
              }).toList(),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildReminderCard(Reminder reminder) {
    final theme = Theme.of(context);
    final priorityColor = _getPriorityColor(reminder.priority);
    final categoryIcon = _getCategoryIcon(reminder.category);

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      color: reminder.isCompleted
          ? theme.colorScheme.surfaceVariant.withOpacity(0.3)
          : theme.colorScheme.surfaceVariant,
      elevation: 0,
      clipBehavior: Clip.antiAlias,
      child: IntrinsicHeight(
        child: Row(
          children: [
            // Left priority color stripe
            Container(
              width: 6,
              color: priorityColor,
            ),
            Expanded(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: [
                    // Interactive custom Checkbox
                    IconButton(
                      icon: Icon(
                        reminder.isCompleted ? Icons.check_circle : Icons.radio_button_unchecked,
                        color: reminder.isCompleted ? theme.colorScheme.primary : theme.colorScheme.outline,
                        size: 26,
                      ),
                      onPressed: () => _toggleComplete(reminder.id),
                    ),
                    const SizedBox(width: 8),
                    // Title and Description
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            reminder.title,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: theme.textTheme.titleMedium?.copyWith(
                              fontWeight: FontWeight.bold,
                              decoration: reminder.isCompleted ? TextDecoration.lineThrough : null,
                              color: reminder.isCompleted
                                  ? theme.colorScheme.onSurfaceVariant.withOpacity(0.6)
                                  : theme.colorScheme.onSurface,
                            ),
                          ),
                          if (reminder.description.isNotEmpty) ...[
                            const SizedBox(height: 2),
                            Text(
                              reminder.description,
                              maxLines: 2,
                              overflow: TextOverflow.ellipsis,
                              style: theme.textTheme.bodyMedium?.copyWith(
                                color: theme.colorScheme.onSurfaceVariant.withOpacity(
                                  reminder.isCompleted ? 0.5 : 0.8,
                                ),
                              ),
                            ),
                          ],
                          const SizedBox(height: 8),
                          // Badges Row
                          Row(
                            children: [
                              // Category Badge
                              Container(
                                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                                decoration: BoxDecoration(
                                  color: theme.colorScheme.secondaryContainer.withOpacity(0.5),
                                  borderRadius: BorderRadius.circular(6),
                                ),
                                child: Row(
                                  mainAxisSize: MainAxisSize.min,
                                  children: [
                                    Icon(categoryIcon, size: 12, color: theme.colorScheme.onSecondaryContainer),
                                    const SizedBox(width: 4),
                                    Text(
                                      _formatEnumName(reminder.category.name),
                                      style: theme.textTheme.labelSmall?.copyWith(
                                        color: theme.colorScheme.onSecondaryContainer,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                              if (reminder.dueDate != null) ...[
                                const SizedBox(width: 8),
                                // Due Date Badge
                                Container(
                                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                                  decoration: BoxDecoration(
                                    color: theme.colorScheme.primaryContainer.withOpacity(0.4),
                                    borderRadius: BorderRadius.circular(6),
                                  ),
                                  child: Row(
                                    mainAxisSize: MainAxisSize.min,
                                    children: [
                                      Icon(Icons.event, size: 12, color: theme.colorScheme.onPrimaryContainer),
                                      const SizedBox(width: 4),
                                      Text(
                                        '${DateFormat('MMM dd, yyyy').format(reminder.dueDate!)}${reminder.dueTime != null ? ' at ${reminder.dueTime}' : ''}',
                                        style: theme.textTheme.labelSmall?.copyWith(
                                          color: theme.colorScheme.onPrimaryContainer,
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                              ],
                            ],
                          ),
                        ],
                      ),
                    ),
                    // Delete Button
                    IconButton(
                      icon: Icon(Icons.delete_outline, color: theme.colorScheme.error.withOpacity(0.8)),
                      onPressed: () => _deleteReminder(reminder.id),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildEmptyState() {
    final theme = Theme.of(context);
    final hasActiveFilters = _searchQuery.isNotEmpty ||
        _selectedPriorityFilter != null ||
        _selectedCategoryFilter != null;

    return Center(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              width: 160,
              height: 160,
              decoration: BoxDecoration(
                color: theme.colorScheme.surfaceVariant,
                borderRadius: BorderRadius.circular(24),
              ),
              child: Icon(
                hasActiveFilters ? Icons.filter_alt_off : Icons.task_outlined,
                size: 80,
                color: theme.colorScheme.primary.withOpacity(0.6),
              ),
            ),
            const SizedBox(height: 24),
            Text(
              hasActiveFilters ? 'No Matching Reminders' : 'Capture Your Thoughts',
              style: theme.textTheme.titleLarge?.copyWith(fontWeight: FontWeight.bold),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 8),
            Text(
              hasActiveFilters
                  ? "We couldn't find any reminders matching your search or filters."
                  : "Quickly add and store reminders, grocery list, work syncs, or custom daily tasks.",
              style: theme.textTheme.bodyMedium?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 24),
            if (hasActiveFilters)
              FilledButton.icon(
                onPressed: () {
                  setState(() {
                    _searchQuery = '';
                    _selectedPriorityFilter = null;
                    _selectedCategoryFilter = null;
                    _applyFilters();
                  });
                },
                icon: const Icon(Icons.refresh),
                label: const Text('Reset All Filters'),
              )
            else
              FilledButton.icon(
                onPressed: _showAddReminderDialog,
                icon: const Icon(Icons.add),
                label: const Text('Create a Reminder'),
              ),
          ],
        ),
      ),
    );
  }

  void _showAddReminderDialog() {
    showDialog(
      context: context,
      builder: (context) {
        return AddReminderDialog(
          onSave: (title, description, dueDate, dueTime, priority, category) {
            final newReminder = Reminder(
              id: DateTime.now().millisecondsSinceEpoch,
              title: title,
              description: description,
              dueDate: dueDate,
              dueTime: dueTime,
              priority: priority,
              category: category,
            );
            _addReminder(newReminder);
          },
        );
      },
    );
  }

  Color _getPriorityColor(Priority priority) {
    switch (priority) {
      case Priority.HIGH:
        return const Color(0xFFE57373);
      case Priority.MEDIUM:
        return const Color(0xFFFFB74D);
      case Priority.LOW:
        return const Color(0xFF81C784);
    }
  }

  IconData _getCategoryIcon(Category category) {
    switch (category) {
      case Category.PERSONAL:
        return Icons.person;
      case Category.WORK:
        return Icons.work;
      case Category.SHOPPING:
        return Icons.shopping_cart;
      case Category.HEALTH:
        return Icons.favorite;
      case Category.LIFE:
        return Icons.home;
      case Category.OTHER:
        return Icons.category;
    }
  }

  String _formatEnumName(String name) {
    return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
  }
}

class AddReminderDialog extends StatefulWidget {
  final Function(
    String title,
    String description,
    DateTime? dueDate,
    String? dueTime,
    Priority priority,
    Category category,
  ) onSave;

  const AddReminderDialog({required this.onSave, Key? key}) : super(key: key);

  @override
  State<AddReminderDialog> createState() => _AddReminderDialogState();
}

class _AddReminderDialogState extends State<AddReminderDialog> {
  final _titleController = TextEditingController();
  final _descController = TextEditingController();
  Priority _selectedPriority = Priority.MEDIUM;
  Category _selectedCategory = Category.PERSONAL;

  DateTime? _selectedDate;
  TimeOfDay? _selectedTime;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return AlertDialog(
      title: const Text(
        'New Reminder',
        style: TextStyle(fontWeight: FontWeight.bold),
      ),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            TextField(
              controller: _titleController,
              decoration: const InputDecoration(
                labelText: 'Title *',
                hintText: 'What needs to be done?',
                border: OutlineInputBorder(borderRadius: BorderRadius.all(Radius.circular(12))),
              ),
              onChanged: (_) => setState(() {}),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: _descController,
              decoration: const InputDecoration(
                labelText: 'Details (Optional)',
                hintText: 'Add more information...',
                border: OutlineInputBorder(borderRadius: BorderRadius.all(Radius.circular(12))),
              ),
              maxLines: 3,
            ),
            const SizedBox(height: 16),
            Text(
              'Priority',
              style: theme.textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            Row(
              children: Priority.values.map((priority) {
                final isSelected = _selectedPriority == priority;
                final priorityColor = _getPriorityColor(priority);
                return Expanded(
                  child: Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 4),
                    child: InkWell(
                      onTap: () {
                        setState(() {
                          _selectedPriority = priority;
                        });
                      },
                      borderRadius: BorderRadius.circular(10),
                      child: Container(
                        padding: const EdgeInsets.symmetric(vertical: 10),
                        decoration: BoxDecoration(
                          color: isSelected ? priorityColor.withOpacity(0.25) : theme.colorScheme.surfaceVariant.withOpacity(0.4),
                          borderRadius: BorderRadius.circular(10),
                          border: Border.all(
                            color: isSelected ? priorityColor : Colors.transparent,
                            width: 1.5,
                          ),
                        ),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            CircleAvatar(backgroundColor: priorityColor, radius: 4),
                            const SizedBox(width: 6),
                            Text(
                              priority.name,
                              style: TextStyle(
                                fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                                color: isSelected ? theme.colorScheme.onSurface : theme.colorScheme.onSurfaceVariant,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                );
              }).toList(),
            ),
            const SizedBox(height: 16),
            Text(
              'Category',
              style: theme.textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            Wrap(
              spacing: 6,
              runSpacing: 6,
              children: Category.values.map((category) {
                final isSelected = _selectedCategory == category;
                final categoryIcon = _getCategoryIcon(category);
                return InkWell(
                  onTap: () {
                    setState(() {
                      _selectedCategory = category;
                    });
                  },
                  borderRadius: BorderRadius.circular(10),
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                    decoration: BoxDecoration(
                      color: isSelected ? theme.colorScheme.primary.withOpacity(0.15) : theme.colorScheme.surfaceVariant.withOpacity(0.4),
                      borderRadius: BorderRadius.circular(10),
                      border: Border.all(
                        color: isSelected ? theme.colorScheme.primary : Colors.transparent,
                        width: 1.5,
                      ),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(
                          categoryIcon,
                          size: 16,
                          color: isSelected ? theme.colorScheme.primary : theme.colorScheme.onSurfaceVariant,
                        ),
                        const SizedBox(width: 6),
                        Text(
                          _formatEnumName(category.name),
                          style: TextStyle(
                            fontSize: 12,
                            fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                            color: isSelected ? theme.colorScheme.onSurface : theme.colorScheme.onSurfaceVariant,
                          ),
                        ),
                      ],
                    ),
                  ),
                );
              }).toList(),
            ),
            const SizedBox(height: 16),
            Text(
              'Schedule',
              style: theme.textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: () async {
                      final picked = await showDatePicker(
                        context: context,
                        initialDate: _selectedDate ?? DateTime.now(),
                        firstDate: DateTime.now(),
                        lastDate: DateTime.now().add(const Duration(days: 365)),
                      );
                      if (picked != null) {
                        setState(() {
                          _selectedDate = picked;
                        });
                      }
                    },
                    icon: const Icon(Icons.calendar_today, size: 16),
                    label: Text(
                      _selectedDate == null ? 'Pick Date' : DateFormat('MMM dd, yyyy').format(_selectedDate!),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(fontSize: 12),
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: () async {
                      final picked = await showTimePicker(
                        context: context,
                        initialTime: _selectedTime ?? TimeOfDay.now(),
                      );
                      if (picked != null) {
                        setState(() {
                          _selectedTime = picked;
                        });
                      }
                    },
                    icon: const Icon(Icons.access_time, size: 16),
                    label: Text(
                      _selectedTime == null ? 'Pick Time' : _selectedTime!.format(context),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(fontSize: 12),
                    ),
                  ),
                ),
              ],
            ),
            if (_selectedDate != null || _selectedTime != null)
              Align(
                alignment: Alignment.centerRight,
                child: TextButton.icon(
                  onPressed: () {
                    setState(() {
                      _selectedDate = null;
                      _selectedTime = null;
                    });
                  },
                  icon: const Icon(Icons.clear, size: 14),
                  label: const Text('Clear Schedule', style: TextStyle(fontSize: 11)),
                ),
              ),
          ],
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('Cancel'),
        ),
        FilledButton(
          onPressed: _titleController.text.trim().isEmpty
              ? null
              : () {
                  String? dueTimeStr;
                  if (_selectedTime != null) {
                    dueTimeStr = '${_selectedTime!.hour.toString().padLeft(2, '0')}:${_selectedTime!.minute.toString().padLeft(2, '0')}';
                  }
                  widget.onSave(
                    _titleController.text.trim(),
                    _descController.text.trim(),
                    _selectedDate,
                    dueTimeStr,
                    _selectedPriority,
                    _selectedCategory,
                  );
                  Navigator.of(context).pop();
                },
          child: const Text('Save'),
        ),
      ],
    );
  }

  Color _getPriorityColor(Priority priority) {
    switch (priority) {
      case Priority.HIGH:
        return const Color(0xFFE57373);
      case Priority.MEDIUM:
        return const Color(0xFFFFB74D);
      case Priority.LOW:
        return const Color(0xFF81C784);
    }
  }

  IconData _getCategoryIcon(Category category) {
    switch (category) {
      case Category.PERSONAL:
        return Icons.person;
      case Category.WORK:
        return Icons.work;
      case Category.SHOPPING:
        return Icons.shopping_cart;
      case Category.HEALTH:
        return Icons.favorite;
      case Category.LIFE:
        return Icons.home;
      case Category.OTHER:
        return Icons.category;
    }
  }

  String _formatEnumName(String name) {
    return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
  }
}
