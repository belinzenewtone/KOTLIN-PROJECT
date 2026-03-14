import 'package:beltech/data/local/drift/app_drift_store.dart';
import 'package:beltech/data/local/drift/app_drift_store_mutations.dart';
import 'package:beltech/features/calendar/domain/entities/calendar_event.dart';
import 'package:beltech/features/calendar/domain/repositories/calendar_repository.dart';

class CalendarRepositoryImpl implements CalendarRepository {
  CalendarRepositoryImpl(this._store);

  final AppDriftStore _store;

  @override
  Stream<List<CalendarEvent>> watchEventsForDay(DateTime day) {
    return _store.watchEventsForDay(day).map(
          (rows) => rows
              .map(
                (row) => CalendarEvent(
                  id: row.id,
                  title: row.title,
                  startAt: row.startAt,
                  completed: row.completed,
                  priority: _priorityFrom(row.priority),
                  type: calendarEventTypeFromRaw(row.eventType),
                  endAt: row.endAt,
                  note: row.note,
                ),
              )
              .toList(),
        );
  }

  @override
  Stream<List<CalendarEvent>> watchEventsInRange(DateTime start, DateTime end) {
    return _store.watchEventsInRange(start, end).map(
          (rows) => rows
              .map(
                (row) => CalendarEvent(
                  id: row.id,
                  title: row.title,
                  startAt: row.startAt,
                  completed: row.completed,
                  priority: _priorityFrom(row.priority),
                  type: calendarEventTypeFromRaw(row.eventType),
                  endAt: row.endAt,
                  note: row.note,
                ),
              )
              .toList(),
        );
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
    await _store.addEvent(
      title: title,
      startAt: startAt,
      priority: priority.name,
      eventType: calendarEventTypeToRaw(type),
      endAt: endAt,
      note: note,
    );
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
  }) {
    return _store.updateEvent(
      id: eventId,
      title: title,
      startAt: startAt,
      priority: priority.name,
      eventType: calendarEventTypeToRaw(type),
      endAt: endAt,
      note: note,
    );
  }

  @override
  Future<void> setCompleted({
    required int eventId,
    required bool completed,
  }) {
    return _store.setEventCompletion(
      eventId: eventId,
      completed: completed,
    );
  }

  @override
  Future<void> deleteEvent(int eventId) {
    return _store.deleteEvent(eventId);
  }

  CalendarEventPriority _priorityFrom(String raw) {
    return switch (raw.toLowerCase()) {
      'high' => CalendarEventPriority.high,
      'low' => CalendarEventPriority.low,
      _ => CalendarEventPriority.medium,
    };
  }
}
