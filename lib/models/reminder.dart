import 'dart:convert';

enum Priority { HIGH, MEDIUM, LOW }

enum Category { PERSONAL, WORK, SHOPPING, HEALTH, LIFE, OTHER }

class Reminder {
  final int id;
  final String title;
  final String description;
  final bool isCompleted;
  final Priority priority;
  final Category category;
  final DateTime? dueDate;
  final String? dueTime;

  Reminder({
    required this.id,
    required this.title,
    required this.description,
    this.isCompleted = false,
    required this.priority,
    required this.category,
    this.dueDate,
    this.dueTime,
  });

  Reminder copyWith({
    int? id,
    String? title,
    String? description,
    bool? isCompleted,
    Priority? priority,
    Category? category,
    DateTime? dueDate,
    String? dueTime,
  }) {
    return Reminder(
      id: id ?? this.id,
      title: title ?? this.title,
      description: description ?? this.description,
      isCompleted: isCompleted ?? this.isCompleted,
      priority: priority ?? this.priority,
      category: category ?? this.category,
      dueDate: dueDate ?? this.dueDate,
      dueTime: dueTime ?? this.dueTime,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'title': title,
      'description': description,
      'isCompleted': isCompleted ? 1 : 0,
      'priority': priority.name,
      'category': category.name,
      'dueDate': dueDate?.millisecondsSinceEpoch,
      'dueTime': dueTime,
    };
  }

  factory Reminder.fromMap(Map<String, dynamic> map) {
    return Reminder(
      id: map['id'] as int,
      title: map['title'] as String,
      description: map['description'] as String,
      isCompleted: (map['isCompleted'] as int) == 1,
      priority: Priority.values.firstWhere((e) => e.name == map['priority']),
      category: Category.values.firstWhere((e) => e.name == map['category']),
      dueDate: map['dueDate'] != null ? DateTime.fromMillisecondsSinceEpoch(map['dueDate'] as int) : null,
      dueTime: map['dueTime'] as String?,
    );
  }

  String toJson() => json.encode(toMap());

  factory Reminder.fromJson(String source) => Reminder.fromMap(json.decode(source) as Map<String, dynamic>);
}
