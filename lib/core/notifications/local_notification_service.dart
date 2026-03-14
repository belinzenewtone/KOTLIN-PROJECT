import 'dart:async';

import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_timezone/flutter_timezone.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:timezone/data/latest.dart' as tz_data;
import 'package:timezone/timezone.dart' as tz;

class LocalNotificationService {
  LocalNotificationService({FlutterLocalNotificationsPlugin? plugin})
      : _plugin = plugin ?? FlutterLocalNotificationsPlugin();

  // ── Notification ID namespaces ────────────────────────────────────────────
  // IDs are computed via FNV-1a hash of (namespace + ":" + recordId), so each
  // (namespace, recordId) pair maps to a unique, stable positive int32. This
  // eliminates the collision risk of the old additive-offset approach (which
  // would collide whenever a taskId exceeded 100,000).
  static const String _nsTask = 'task';
  static const String _nsEvent = 'event';
  static const String _nsInsight = 'insight';

  /// Deterministic FNV-1a hash of [namespace]:[recordId] → positive int32.
  static int _notifId(String namespace, int recordId) {
    var hash = 0x811c9dc5; // FNV-1a 32-bit offset basis
    void fnvByte(int byte) {
      hash ^= byte;
      hash = (hash * 0x01000193) & 0xFFFFFFFF; // FNV prime
    }
    for (final c in namespace.codeUnits) {
      fnvByte(c);
    }
    fnvByte(0x3A); // ':' separator
    var id = recordId;
    do {
      fnvByte(id & 0xFF);
      id >>= 8;
    } while (id > 0);
    return hash & 0x7FFFFFFF; // ensure positive (signed int32 safe)
  }

  static const String _channelId = 'task_event_reminders';
  static const String _channelName = 'Task and Event Reminders';
  static const String _channelDescription =
      'Notifications for task deadlines and calendar events.';
  static const String _notificationsEnabledKey = 'notifications_enabled';

  final FlutterLocalNotificationsPlugin _plugin;
  bool _initialized = false;

  Future<void> scheduleTaskReminder({
    required int taskId,
    required String title,
    required DateTime dueDate,
  }) async {
    final hasExplicitTime = dueDate.hour != 0 || dueDate.minute != 0;
    final reminderAt = hasExplicitTime
        ? dueDate
        : DateTime(
            dueDate.year,
            dueDate.month,
            dueDate.day,
            9,
          );
    await _scheduleAt(
      id: _notifId(_nsTask, taskId),
      title: 'Task Reminder',
      body: '$title is due soon.',
      when: reminderAt,
    );
  }

  Future<void> cancelTaskReminder(int taskId) {
    return _cancelById(_notifId(_nsTask, taskId));
  }

  Future<void> scheduleEventReminder({
    required int eventId,
    required String title,
    required DateTime startAt,
  }) async {
    final now = DateTime.now();
    final fifteenBefore = startAt.subtract(const Duration(minutes: 15));
    final reminderAt = fifteenBefore.isAfter(now) ? fifteenBefore : startAt;
    await _scheduleAt(
      id: _notifId(_nsEvent, eventId),
      title: 'Upcoming Event',
      body: '$title starts soon.',
      when: reminderAt,
    );
  }

  Future<void> cancelEventReminder(int eventId) {
    return _cancelById(_notifId(_nsEvent, eventId));
  }

  Future<void> showInsight({
    required int insightId,
    required String title,
    required String body,
  }) async {
    final enabled = await isNotificationsEnabled();
    if (!enabled) {
      return;
    }
    await _ensureInitialized();
    await _plugin.show(
      id: _notifId(_nsInsight, insightId),
      title: title,
      body: body,
      notificationDetails: _details,
    );
  }

  Future<bool> isNotificationsEnabled() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getBool(_notificationsEnabledKey) ?? true;
  }

  Future<void> setNotificationsEnabled(bool enabled) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_notificationsEnabledKey, enabled);
    if (!enabled) {
      await cancelAllReminders();
    }
  }

  Future<void> cancelAllReminders() async {
    await _ensureInitialized();
    await _plugin.cancelAll();
  }

  Future<void> cleanupOrphanedReminders({
    required Iterable<int> activeTaskIds,
    required Iterable<int> activeEventIds,
  }) async {
    await _ensureInitialized();
    final pending = await _plugin.pendingNotificationRequests();

    // Build the complete set of IDs that should currently be scheduled.
    // Insights use show() (immediate) so they never appear in pending — safe
    // to cancel anything not in this whitelist.
    final validIds = <int>{
      for (final id in activeTaskIds) _notifId(_nsTask, id),
      for (final id in activeEventIds) _notifId(_nsEvent, id),
    };

    for (final item in pending) {
      if (!validIds.contains(item.id)) {
        await _plugin.cancel(id: item.id);
      }
    }
  }

  Future<void> _scheduleAt({
    required int id,
    required String title,
    required String body,
    required DateTime when,
  }) async {
    final enabled = await isNotificationsEnabled();
    if (!enabled) {
      return;
    }
    if (!when.isAfter(DateTime.now())) {
      return;
    }
    await _ensureInitialized();
    await _plugin.zonedSchedule(
      id: id,
      title: title,
      body: body,
      scheduledDate: tz.TZDateTime.from(when, tz.local),
      notificationDetails: _details,
      androidScheduleMode: AndroidScheduleMode.inexactAllowWhileIdle,
    );
  }

  Future<void> _cancelById(int id) async {
    await _ensureInitialized();
    await _plugin.cancel(id: id);
  }

  Future<void> _ensureInitialized() async {
    if (_initialized) {
      return;
    }
    if (kIsWeb) {
      _initialized = true;
      return;
    }

    tz_data.initializeTimeZones();
    try {
      final zone = await FlutterTimezone.getLocalTimezone();
      tz.setLocalLocation(tz.getLocation(zone.identifier));
    } catch (_) {
      tz.setLocalLocation(tz.UTC);
    }

    const initSettings = InitializationSettings(
      android: AndroidInitializationSettings('@mipmap/ic_launcher'),
      iOS: DarwinInitializationSettings(
        requestAlertPermission: false,
        requestBadgePermission: false,
        requestSoundPermission: false,
      ),
      macOS: DarwinInitializationSettings(
        requestAlertPermission: false,
        requestBadgePermission: false,
        requestSoundPermission: false,
      ),
      linux: LinuxInitializationSettings(defaultActionName: 'Open'),
      windows: WindowsInitializationSettings(
        appName: 'BELTECH',
        appUserModelId: 'beltech.app',
        guid: 'cd8f4c25-95e8-420f-b74b-c30db7b8e8c9',
      ),
    );

    await _plugin.initialize(settings: initSettings);

    if (defaultTargetPlatform == TargetPlatform.android) {
      await _plugin
          .resolvePlatformSpecificImplementation<
              AndroidFlutterLocalNotificationsPlugin>()
          ?.requestNotificationsPermission();
    } else if (defaultTargetPlatform == TargetPlatform.iOS) {
      await _plugin
          .resolvePlatformSpecificImplementation<
              IOSFlutterLocalNotificationsPlugin>()
          ?.requestPermissions(alert: true, badge: true, sound: true);
    } else if (defaultTargetPlatform == TargetPlatform.macOS) {
      await _plugin
          .resolvePlatformSpecificImplementation<
              MacOSFlutterLocalNotificationsPlugin>()
          ?.requestPermissions(alert: true, badge: true, sound: true);
    }

    _initialized = true;
  }

  NotificationDetails get _details {
    const android = AndroidNotificationDetails(
      _channelId,
      _channelName,
      channelDescription: _channelDescription,
      importance: Importance.high,
      priority: Priority.high,
      playSound: true,
      enableVibration: true,
      silent: false,
    );
    const darwin = DarwinNotificationDetails(
      presentAlert: true,
      presentBadge: true,
      presentSound: true,
    );
    return const NotificationDetails(
      android: android,
      iOS: darwin,
      macOS: darwin,
    );
  }
}
