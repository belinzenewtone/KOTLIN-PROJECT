import 'package:beltech/core/theme/app_colors.dart';
import 'package:beltech/core/theme/app_spacing.dart';
import 'package:beltech/core/widgets/app_empty_state.dart';
import 'package:beltech/core/widgets/app_feedback.dart';
import 'package:beltech/core/widgets/app_skeleton.dart';
import 'package:beltech/core/widgets/error_message.dart';
import 'package:beltech/core/widgets/glass_card.dart';
import 'package:beltech/core/widgets/page_header.dart';
import 'package:beltech/core/widgets/page_shell.dart';
import 'package:beltech/features/calendar/domain/entities/calendar_event.dart';
import 'package:beltech/features/calendar/presentation/providers/calendar_providers.dart';
import 'package:beltech/features/calendar/presentation/widgets/calendar_events_card.dart';
import 'package:beltech/features/calendar/presentation/widgets/calendar_month_grid.dart';
import 'package:beltech/features/calendar/presentation/widgets/event_dialogs.dart';
import 'package:beltech/features/search/domain/entities/global_search_result.dart';
import 'package:beltech/features/search/presentation/providers/global_search_providers.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

part 'calendar_screen_events.dart';
part 'calendar_screen_layout.dart';

enum _CalendarView { month, week }

class CalendarScreen extends ConsumerStatefulWidget {
  const CalendarScreen({super.key});

  @override
  ConsumerState<CalendarScreen> createState() => _CalendarScreenState();
}

class _CalendarScreenState extends ConsumerState<CalendarScreen> {
  _CalendarView _view = _CalendarView.month;
  bool _swiping = false; // blocks events list during a calendar swipe
  static const double _calendarContentMaxWidth = 360;

  static const List<String> _weekDays = [
    'Mo',
    'Tu',
    'We',
    'Th',
    'Fr',
    'Sa',
    'Su'
  ];
  static const List<String> _months = [
    'January',
    'February',
    'March',
    'April',
    'May',
    'June',
    'July',
    'August',
    'September',
    'October',
    'November',
    'December',
  ];

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final visibleMonth = ref.watch(visibleMonthProvider);
    final selectedDay = ref.watch(selectedDayProvider);
    final eventsState = ref.watch(dayEventsProvider);
    final monthEventTypesState = ref.watch(monthEventTypesProvider);
    final writeState = ref.watch(calendarWriteControllerProvider);
    final eventsPaneHeight =
        (MediaQuery.sizeOf(context).height * 0.36).clamp(240.0, 380.0);
    _syncSearchTargetDay(ref, selectedDay);

    ref.listen<AsyncValue<void>>(calendarWriteControllerProvider,
        (previous, next) {
      if (next.hasError) {
        AppFeedback.error(context, 'Unable to save calendar event.', ref: ref);
      }
    });

    final title = _view == _CalendarView.month
        ? '${_months[visibleMonth.month - 1]} ${visibleMonth.year}'
        : _weekRangeLabel(selectedDay);

    // Week strip: 7 days starting from Monday of selected week
    final weekStart =
        selectedDay.subtract(Duration(days: selectedDay.weekday - 1));
    final weekDays = List.generate(
      7,
      (i) => weekStart.add(Duration(days: i)),
    );

    return _CalendarLayout(
      state: this,
      textTheme: textTheme,
      visibleMonth: visibleMonth,
      selectedDay: selectedDay,
      eventsState: eventsState,
      monthEventTypesState: monthEventTypesState,
      writeState: writeState,
      eventsPaneHeight: eventsPaneHeight,
      title: title,
      weekDays: weekDays,
    );
  }

  void _setView(_CalendarView view) {
    setState(() => _view = view);
  }

  void _beginSwipe() {
    setState(() => _swiping = true);
  }

  void _cancelSwipe() {
    setState(() => _swiping = false);
  }

  void _handleSwipeEnd(DragEndDetails details) {
    setState(() => _swiping = false);
    final velocity = details.primaryVelocity ?? 0;
    if (_view == _CalendarView.month) {
      if (velocity < -120) _changeMonth(ref, 1);
      if (velocity > 120) _changeMonth(ref, -1);
      return;
    }
    if (velocity < -120) _changeWeek(ref, 1);
    if (velocity > 120) _changeWeek(ref, -1);
  }

  void _syncSearchTargetDay(WidgetRef ref, DateTime selectedDay) {
    final target = ref.read(globalSearchDeepLinkTargetProvider);
    if (target?.kind != GlobalSearchKind.event) {
      return;
    }
    final recordDate = target?.recordDate;
    if (recordDate == null) {
      return;
    }
    final normalized =
        DateTime(recordDate.year, recordDate.month, recordDate.day);
    if (_isSameDate(normalized, selectedDay)) {
      return;
    }
    ref.read(selectedDayProvider.notifier).state = normalized;
    ref.read(visibleMonthProvider.notifier).state =
        DateTime(normalized.year, normalized.month, 1);
  }

  void _consumeSearchTarget(
    BuildContext context,
    WidgetRef ref,
    DateTime selectedDay,
    List<CalendarEvent> events,
  ) {
    final target = ref.read(globalSearchDeepLinkTargetProvider);
    if (target?.kind != GlobalSearchKind.event) {
      return;
    }
    final recordDate = target?.recordDate;
    if (recordDate != null && !_isSameDate(recordDate, selectedDay)) {
      return;
    }

    ref.read(globalSearchDeepLinkTargetProvider.notifier).state = null;

    final recordId = target?.recordId;
    if (recordId == null) {
      return;
    }
    final event = events.where((item) => item.id == recordId).firstOrNull;
    if (event == null) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (context.mounted) {
          AppFeedback.info(context, 'This calendar event no longer exists.',
              ref: ref);
        }
      });
      return;
    }

    WidgetsBinding.instance.addPostFrameCallback((_) async {
      if (!context.mounted) {
        return;
      }
      final input = await showEditEventDialog(
        context,
        selectedDay: selectedDay,
        event: event,
      );
      if (input == null) {
        return;
      }
      await ref.read(calendarWriteControllerProvider.notifier).updateEvent(
            eventId: event.id,
            title: input.title,
            startAt: input.startAt,
            priority: input.priority,
            type: input.type,
            endAt: input.endAt,
            note: input.note,
          );
      if (context.mounted &&
          !ref.read(calendarWriteControllerProvider).hasError) {
        AppFeedback.success(context, 'Event updated', ref: ref);
      }
    });
  }

  bool _isSameDate(DateTime a, DateTime b) {
    return a.year == b.year && a.month == b.month && a.day == b.day;
  }

  void _changeMonth(WidgetRef ref, int offset) {
    final visible = ref.read(visibleMonthProvider);
    final next = DateTime(visible.year, visible.month + offset, 1);
    ref.read(visibleMonthProvider.notifier).state = next;
    final selected = ref.read(selectedDayProvider);
    if (selected.year != next.year || selected.month != next.month) {
      ref.read(selectedDayProvider.notifier).state =
          DateTime(next.year, next.month, 1);
    }
  }

  void _changeWeek(WidgetRef ref, int offset) {
    final selected = ref.read(selectedDayProvider);
    final next = selected.add(Duration(days: 7 * offset));
    ref.read(selectedDayProvider.notifier).state = next;
    // Keep visible month in sync
    if (next.year != ref.read(visibleMonthProvider).year ||
        next.month != ref.read(visibleMonthProvider).month) {
      ref.read(visibleMonthProvider.notifier).state =
          DateTime(next.year, next.month, 1);
    }
  }

  String _weekRangeLabel(DateTime day) {
    final start = day.subtract(Duration(days: day.weekday - 1));
    final end = start.add(const Duration(days: 6));
    final startStr = '${_months[start.month - 1].substring(0, 3)} ${start.day}';
    final endStr = start.month == end.month
        ? '${end.day}'
        : '${_months[end.month - 1].substring(0, 3)} ${end.day}';
    return '$startStr – $endStr, ${end.year}';
  }

  String _weekdayName(int weekday) {
    const weekdays = [
      'Monday',
      'Tuesday',
      'Wednesday',
      'Thursday',
      'Friday',
      'Saturday',
      'Sunday',
    ];
    return weekdays[weekday - 1];
  }
}
