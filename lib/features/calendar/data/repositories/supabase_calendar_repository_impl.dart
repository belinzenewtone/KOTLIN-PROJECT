import 'package:beltech/data/remote/supabase/supabase_parsers.dart';
import 'package:beltech/data/remote/supabase/supabase_polling.dart';
import 'package:beltech/features/calendar/domain/entities/calendar_event.dart';
import 'package:beltech/features/calendar/domain/repositories/calendar_repository.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

class SupabaseCalendarRepositoryImpl implements CalendarRepository {
  SupabaseCalendarRepositoryImpl(this._client);

  final SupabaseClient _client;

  @override
  Stream<List<CalendarEvent>> watchEventsForDay(DateTime day) {
    final start = DateTime(day.year, day.month, day.day);
    final end = start.add(const Duration(days: 1));
    return pollStream(
      () => _loadForRange(start, end),
    );
  }

  @override
  Stream<List<CalendarEvent>> watchEventsInRange(DateTime start, DateTime end) {
    return pollStream(
      () => _loadForRange(start, end),
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
  }) {
    final userId = _requireUserId();
    return _client.from('events').insert({
      'owner_id': userId,
      'title': title,
      'start_at': startAt.toUtc().toIso8601String(),
      'end_at': endAt?.toUtc().toIso8601String(),
      'note': note,
      'completed': false,
      'priority': priority.name,
      'event_type': calendarEventTypeToRaw(type),
    });
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
    final userId = _requireUserId();
    return _client
        .from('events')
        .update({
          'title': title,
          'start_at': startAt.toUtc().toIso8601String(),
          'end_at': endAt?.toUtc().toIso8601String(),
          'note': note,
          'priority': priority.name,
          'event_type': calendarEventTypeToRaw(type),
        })
        .eq('id', eventId)
        .eq('owner_id', userId);
  }

  @override
  Future<void> setCompleted({
    required int eventId,
    required bool completed,
  }) {
    final userId = _requireUserId();
    return _client
        .from('events')
        .update({'completed': completed})
        .eq('id', eventId)
        .eq('owner_id', userId);
  }

  @override
  Future<void> deleteEvent(int eventId) {
    final userId = _requireUserId();
    return _client
        .from('events')
        .delete()
        .eq('id', eventId)
        .eq('owner_id', userId);
  }

  Future<List<CalendarEvent>> _loadForRange(
      DateTime start, DateTime end) async {
    final userId = _requireUserId();
    final rows = await _client
        .from('events')
        .select('id,title,start_at,end_at,note,completed,priority,event_type')
        .eq('owner_id', userId)
        .gte('start_at', start.toUtc().toIso8601String())
        .lt('start_at', end.toUtc().toIso8601String())
        .order('completed')
        .order('start_at');
    final events = (rows as List).cast<Map<String, dynamic>>();
    return events
        .map(
          (row) => CalendarEvent(
            id: parseInt(row['id']),
            title: '${row['title'] ?? ''}',
            startAt: parseTimestamp(row['start_at']),
            completed: row['completed'] == true,
            priority: _priorityFrom('${row['priority'] ?? 'medium'}'),
            type: calendarEventTypeFromRaw('${row['event_type'] ?? 'general'}'),
            endAt: row['end_at'] == null ? null : parseTimestamp(row['end_at']),
            note: row['note'] as String?,
          ),
        )
        .toList();
  }

  CalendarEventPriority _priorityFrom(String raw) {
    return switch (raw.toLowerCase()) {
      'high' => CalendarEventPriority.high,
      'low' => CalendarEventPriority.low,
      _ => CalendarEventPriority.medium,
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
