import 'dart:async';

import 'package:beltech/core/di/notification_providers.dart';
import 'package:beltech/core/di/repository_providers.dart';
import 'package:beltech/core/notifications/local_notification_service.dart';
import 'package:beltech/features/calendar/domain/entities/calendar_event.dart';
import 'package:beltech/features/calendar/domain/repositories/calendar_repository.dart';
import 'package:beltech/features/calendar/presentation/providers/calendar_providers.dart';
import 'package:beltech/features/tasks/domain/entities/task_item.dart';
import 'package:beltech/features/tasks/domain/repositories/tasks_repository.dart';
import 'package:beltech/features/tasks/presentation/providers/tasks_providers.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('task add/update triggers schedule and cancel hooks', () async {
    final tasksRepo = _FakeTasksRepository();
    final notifications = _FakeLocalNotificationService();
    final container = ProviderContainer(
      overrides: [
        tasksRepositoryProvider.overrideWithValue(tasksRepo),
        localNotificationServiceProvider.overrideWithValue(notifications),
      ],
    );
    addTearDown(container.dispose);

    final due = DateTime.now().add(const Duration(days: 1));
    await container.read(taskWriteControllerProvider.notifier).addTask(
          title: 'Reminder Task',
          dueDate: due,
          priority: TaskPriority.high,
        );

    expect(notifications.scheduledTaskIds, contains(1));

    await container.read(taskWriteControllerProvider.notifier).updateTask(
          taskId: 1,
          title: 'Reminder Task',
          dueDate: null,
          priority: TaskPriority.high,
        );

    expect(notifications.canceledTaskIds, contains(1));
  });

  test('event add/delete triggers schedule and cancel hooks', () async {
    final calendarRepo = _FakeCalendarRepository();
    final notifications = _FakeLocalNotificationService();
    final container = ProviderContainer(
      overrides: [
        calendarRepositoryProvider.overrideWithValue(calendarRepo),
        localNotificationServiceProvider.overrideWithValue(notifications),
      ],
    );
    addTearDown(container.dispose);

    final day = DateTime.now().add(const Duration(days: 1));
    final start = DateTime(day.year, day.month, day.day, 11);
    await container.read(calendarWriteControllerProvider.notifier).addEvent(
          title: 'Team Call',
          startAt: start,
          priority: CalendarEventPriority.medium,
          type: CalendarEventType.work,
          endAt: start.add(const Duration(hours: 1)),
          note: 'Planning',
        );

    expect(notifications.scheduledEventIds, contains(1));

    await container
        .read(calendarWriteControllerProvider.notifier)
        .deleteEvent(1);
    expect(notifications.canceledEventIds, contains(1));
  });
}

class _FakeLocalNotificationService extends LocalNotificationService {
  final List<int> scheduledTaskIds = [];
  final List<int> canceledTaskIds = [];
  final List<int> scheduledEventIds = [];
  final List<int> canceledEventIds = [];

  @override
  Future<void> scheduleTaskReminder({
    required int taskId,
    required String title,
    required DateTime dueDate,
  }) async {
    scheduledTaskIds.add(taskId);
  }

  @override
  Future<void> cancelTaskReminder(int taskId) async {
    canceledTaskIds.add(taskId);
  }

  @override
  Future<void> scheduleEventReminder({
    required int eventId,
    required String title,
    required DateTime startAt,
  }) async {
    scheduledEventIds.add(eventId);
  }

  @override
  Future<void> cancelEventReminder(int eventId) async {
    canceledEventIds.add(eventId);
  }
}

class _FakeTasksRepository implements TasksRepository {
  final StreamController<void> _changes = StreamController<void>.broadcast();
  final List<TaskItem> _tasks = [];
  int _nextId = 1;

  @override
  Stream<List<TaskItem>> watchTasks() {
    return Stream<List<TaskItem>>.multi((controller) {
      controller.add(List.unmodifiable(_tasks));
      final sub = _changes.stream.listen((_) {
        controller.add(List.unmodifiable(_tasks));
      });
      controller.onCancel = sub.cancel;
    });
  }

  @override
  Future<void> addTask({
    required String title,
    String? description,
    DateTime? dueDate,
    TaskPriority priority = TaskPriority.medium,
  }) async {
    _tasks.insert(
      0,
      TaskItem(
        id: _nextId++,
        title: title,
        description: description,
        completed: false,
        priority: priority,
        dueDate: dueDate,
      ),
    );
    _changes.add(null);
  }

  @override
  Future<void> toggleCompleted({
    required int taskId,
    required bool completed,
  }) async {
    final index = _tasks.indexWhere((item) => item.id == taskId);
    if (index == -1) {
      return;
    }
    final current = _tasks[index];
    _tasks[index] = TaskItem(
      id: current.id,
      title: current.title,
      description: current.description,
      completed: completed,
      priority: current.priority,
      dueDate: current.dueDate,
    );
    _changes.add(null);
  }

  @override
  Future<void> updateTask({
    required int taskId,
    required String title,
    String? description,
    required DateTime? dueDate,
    required TaskPriority priority,
  }) async {
    final index = _tasks.indexWhere((item) => item.id == taskId);
    if (index == -1) {
      return;
    }
    final current = _tasks[index];
    _tasks[index] = TaskItem(
      id: taskId,
      title: title,
      description: description ?? current.description,
      completed: current.completed,
      priority: priority,
      dueDate: dueDate,
    );
    _changes.add(null);
  }

  @override
  Future<void> deleteTask(int taskId) async {
    _tasks.removeWhere((item) => item.id == taskId);
    _changes.add(null);
  }
}

class _FakeCalendarRepository implements CalendarRepository {
  final StreamController<void> _changes = StreamController<void>.broadcast();
  final List<CalendarEvent> _events = [];
  int _nextId = 1;

  @override
  Stream<List<CalendarEvent>> watchEventsForDay(DateTime day) {
    return Stream<List<CalendarEvent>>.multi((controller) {
      controller.add(_eventsFor(day));
      final sub = _changes.stream.listen((_) {
        controller.add(_eventsFor(day));
      });
      controller.onCancel = sub.cancel;
    });
  }

  @override
  Stream<List<CalendarEvent>> watchEventsInRange(DateTime start, DateTime end) {
    return Stream<List<CalendarEvent>>.multi((controller) {
      controller.add(_eventsInRange(start, end));
      final sub = _changes.stream.listen((_) {
        controller.add(_eventsInRange(start, end));
      });
      controller.onCancel = sub.cancel;
    });
  }

  @override
  Future<void> addEvent({
    required String title,
    required DateTime startAt,
    CalendarEventPriority priority = CalendarEventPriority.medium,
    CalendarEventType type = CalendarEventType.general,
    DateTime? endAt,
    String? note,
  }) async {
    _events.add(
      CalendarEvent(
        id: _nextId++,
        title: title,
        startAt: startAt,
        completed: false,
        priority: priority,
        type: type,
        endAt: endAt,
        note: note,
      ),
    );
    _changes.add(null);
  }

  @override
  Future<void> updateEvent({
    required int eventId,
    required String title,
    required DateTime startAt,
    required CalendarEventPriority priority,
    required CalendarEventType type,
    DateTime? endAt,
    String? note,
  }) async {
    final index = _events.indexWhere((item) => item.id == eventId);
    if (index == -1) {
      return;
    }
    _events[index] = CalendarEvent(
      id: eventId,
      title: title,
      startAt: startAt,
      completed: _events[index].completed,
      priority: priority,
      type: type,
      endAt: endAt,
      note: note,
    );
    _changes.add(null);
  }

  @override
  Future<void> setCompleted({
    required int eventId,
    required bool completed,
  }) async {
    final index = _events.indexWhere((item) => item.id == eventId);
    if (index == -1) {
      return;
    }
    final current = _events[index];
    _events[index] = CalendarEvent(
      id: current.id,
      title: current.title,
      startAt: current.startAt,
      completed: completed,
      priority: current.priority,
      type: current.type,
      endAt: current.endAt,
      note: current.note,
    );
    _changes.add(null);
  }

  @override
  Future<void> deleteEvent(int eventId) async {
    _events.removeWhere((item) => item.id == eventId);
    _changes.add(null);
  }

  List<CalendarEvent> _eventsFor(DateTime day) {
    return _events.where((event) {
      return event.startAt.year == day.year &&
          event.startAt.month == day.month &&
          event.startAt.day == day.day;
    }).toList();
  }

  List<CalendarEvent> _eventsInRange(DateTime start, DateTime end) {
    return _events.where((event) {
      return !event.startAt.isBefore(start) && event.startAt.isBefore(end);
    }).toList();
  }
}
