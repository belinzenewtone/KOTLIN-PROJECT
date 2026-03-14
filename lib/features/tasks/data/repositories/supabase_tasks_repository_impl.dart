import 'package:beltech/data/remote/supabase/supabase_parsers.dart';
import 'package:beltech/data/remote/supabase/supabase_polling.dart';
import 'package:beltech/features/tasks/domain/entities/task_item.dart';
import 'package:beltech/features/tasks/domain/repositories/tasks_repository.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

class SupabaseTasksRepositoryImpl implements TasksRepository {
  SupabaseTasksRepositoryImpl(this._client);

  final SupabaseClient _client;

  @override
  Stream<List<TaskItem>> watchTasks() => pollStream(_loadTasks);

  @override
  Future<void> addTask({
    required String title,
    String? description,
    DateTime? dueDate,
    TaskPriority priority = TaskPriority.medium,
  }) {
    final userId = _requireUserId();
    return _client.from('tasks').insert({
      'owner_id': userId,
      'title': title,
      'description': description,
      'completed': false,
      'due_at': dueDate?.toUtc().toIso8601String(),
      'priority': priority.name,
    });
  }

  @override
  Future<void> toggleCompleted({
    required int taskId,
    required bool completed,
  }) {
    final userId = _requireUserId();
    return _client
        .from('tasks')
        .update({'completed': completed})
        .eq('id', taskId)
        .eq('owner_id', userId);
  }

  @override
  Future<void> updateTask({
    required int taskId,
    required String title,
    String? description,
    required DateTime? dueDate,
    required TaskPriority priority,
  }) {
    final userId = _requireUserId();
    return _client
        .from('tasks')
        .update({
          'title': title,
          'description': description,
          'due_at': dueDate?.toUtc().toIso8601String(),
          'priority': priority.name,
        })
        .eq('id', taskId)
        .eq('owner_id', userId);
  }

  @override
  Future<void> deleteTask(int taskId) {
    final userId = _requireUserId();
    return _client
        .from('tasks')
        .delete()
        .eq('id', taskId)
        .eq('owner_id', userId);
  }

  Future<List<TaskItem>> _loadTasks() async {
    final userId = _requireUserId();
    final rows = await _client
        .from('tasks')
        .select('id,title,description,completed,due_at,priority')
        .eq('owner_id', userId)
        .order('id', ascending: false);
    final tasks = (rows as List).cast<Map<String, dynamic>>();
    return tasks
        .map(
          (row) => TaskItem(
            id: parseInt(row['id']),
            title: '${row['title'] ?? ''}',
            description: row['description'] as String?,
            completed: row['completed'] == true,
            priority: _priorityFrom('${row['priority'] ?? 'medium'}'),
            dueDate:
                row['due_at'] == null ? null : parseTimestamp(row['due_at']),
          ),
        )
        .toList();
  }

  TaskPriority _priorityFrom(String raw) {
    return switch (raw.toLowerCase()) {
      'high' => TaskPriority.high,
      'low' => TaskPriority.low,
      _ => TaskPriority.medium,
    };
  }

  String _requireUserId() {
    final userId = _client.auth.currentUser?.id;
    if (userId == null || userId.isEmpty) {
      throw Exception('Sign in is required.');
    }
    return userId;
  }
}
