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

    return PageShell(
      scrollable: true,
      glowColor: AppColors.glowBlue,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          PageHeader(
            eyebrow: 'PLAN',
            title: 'Calendar',
            action: IconButton(
              tooltip: 'Add event',
              onPressed: writeState.isLoading
                  ? null
                  : () async {
                      final input = await showAddEventDialog(
                        context,
                        selectedDay: selectedDay,
                      );
                      if (input == null) {
                        return;
                      }
                      await ref
                          .read(calendarWriteControllerProvider.notifier)
                          .addEvent(
                            title: input.title,
                            startAt: input.startAt,
                            priority: input.priority,
                            type: input.type,
                            endAt: input.endAt,
                            note: input.note,
                          );
                      if (context.mounted &&
                          !ref.read(calendarWriteControllerProvider).hasError) {
                        AppFeedback.success(context, 'Event added', ref: ref);
                      }
                    },
              icon: const Icon(Icons.add_rounded),
            ),
          ),
          Row(
            mainAxisAlignment: MainAxisAlignment.end,
            children: [
              SegmentedButton<_CalendarView>(
                showSelectedIcon: false,
                style: const ButtonStyle(
                  tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                  visualDensity: VisualDensity.compact,
                ),
                segments: const [
                  ButtonSegment(
                    value: _CalendarView.month,
                    icon: Icon(Icons.calendar_month_outlined, size: 18),
                    label: Text('Month'),
                  ),
                  ButtonSegment(
                    value: _CalendarView.week,
                    icon: Icon(Icons.view_week_outlined, size: 18),
                    label: Text('Week'),
                  ),
                ],
                selected: {_view},
                onSelectionChanged: (v) => setState(() => _view = v.first),
              ),
            ],
          ),
          const SizedBox(height: 12),
          GestureDetector(
            behavior: HitTestBehavior.opaque,
            onHorizontalDragStart: (_) => setState(() => _swiping = true),
            onHorizontalDragEnd: (details) {
              setState(() => _swiping = false);
              final velocity = details.primaryVelocity ?? 0;
              if (_view == _CalendarView.month) {
                if (velocity < -120) _changeMonth(ref, 1);
                if (velocity > 120) _changeMonth(ref, -1);
              } else {
                if (velocity < -120) _changeWeek(ref, 1);
                if (velocity > 120) _changeWeek(ref, -1);
              }
            },
            onHorizontalDragCancel: () => setState(() => _swiping = false),
            child: GlassCard(
              child: Column(
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      IconButton(
                        onPressed: () => _view == _CalendarView.month
                            ? _changeMonth(ref, -1)
                            : _changeWeek(ref, -1),
                        icon: const Icon(Icons.chevron_left),
                      ),
                      Text(title, style: textTheme.titleMedium),
                      IconButton(
                        onPressed: () => _view == _CalendarView.month
                            ? _changeMonth(ref, 1)
                            : _changeWeek(ref, 1),
                        icon: const Icon(Icons.chevron_right),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  if (_view == _CalendarView.month) ...[
                    Center(
                      child: ConstrainedBox(
                        constraints: const BoxConstraints(
                          maxWidth: _calendarContentMaxWidth,
                        ),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: _weekDays
                              .map(
                                (day) => SizedBox(
                                  width: 30,
                                  child: Text(
                                    day,
                                    textAlign: TextAlign.center,
                                    style: textTheme.bodyMedium,
                                  ),
                                ),
                              )
                              .toList(),
                        ),
                      ),
                    ),
                    const SizedBox(height: 12),
                    CalendarMonthGrid(
                      visibleMonth: visibleMonth,
                      selectedDay: selectedDay,
                      eventTypes: monthEventTypesState.valueOrNull ?? const {},
                      maxWidth: _calendarContentMaxWidth,
                      onSelect: (day) {
                        ref.read(selectedDayProvider.notifier).state = day;
                      },
                    ),
                  ] else ...[
                    // Week view: 7 day cells in a row
                    LayoutBuilder(
                      builder: (context, constraints) {
                        final daySize = ((constraints.maxWidth / 7) - 10)
                            .clamp(24.0, 34.0)
                            .toDouble();
                        return Row(
                          children: weekDays.map((day) {
                            final isSelected = day.year == selectedDay.year &&
                                day.month == selectedDay.month &&
                                day.day == selectedDay.day;
                            final isToday = day.year == DateTime.now().year &&
                                day.month == DateTime.now().month &&
                                day.day == DateTime.now().day;
                            return Expanded(
                              child: GestureDetector(
                                onTap: () {
                                  ref.read(selectedDayProvider.notifier).state =
                                      day;
                                },
                                child: Column(
                                  children: [
                                    Text(
                                      _weekDays[day.weekday - 1],
                                      style: textTheme.bodySmall,
                                    ),
                                    const SizedBox(height: 4),
                                    Center(
                                      child: Container(
                                        width: daySize,
                                        height: daySize,
                                        decoration: BoxDecoration(
                                          shape: BoxShape.circle,
                                          color: isSelected
                                              ? Theme.of(context)
                                                  .colorScheme
                                                  .primary
                                              : isToday
                                                  ? Theme.of(context)
                                                      .colorScheme
                                                      .primary
                                                      .withValues(alpha: 0.22)
                                                  : Colors.transparent,
                                        ),
                                        child: Center(
                                          child: Text(
                                            '${day.day}',
                                            style:
                                                textTheme.bodyMedium?.copyWith(
                                              fontWeight: isSelected || isToday
                                                  ? FontWeight.w700
                                                  : FontWeight.w400,
                                              color: isSelected
                                                  ? Colors.white
                                                  : null,
                                            ),
                                          ),
                                        ),
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            );
                          }).toList(),
                        );
                      },
                    ),
                    const SizedBox(height: 8),
                  ],
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),
          Text(
            '${_weekdayName(selectedDay.weekday)}, ${_months[selectedDay.month - 1]} ${selectedDay.day.toString().padLeft(2, '0')}',
            style: textTheme.titleMedium,
          ),
          const SizedBox(height: 10),
          SizedBox(
            height: eventsPaneHeight,
            child: AbsorbPointer(
              absorbing: _swiping,
              child: eventsState.when(
                data: (events) {
                  _consumeSearchTarget(context, ref, selectedDay, events);
                  if (events.isEmpty) {
                    return AppEmptyState(
                      icon: Icons.event_outlined,
                      title: 'No events',
                      subtitle: 'Add an event to get started',
                    );
                  }
                  return CalendarEventsCard(
                    events: events,
                    busy: writeState.isLoading,
                    onComplete: (event) async {
                      if (event.completed) {
                        return;
                      }
                      await ref
                          .read(calendarWriteControllerProvider.notifier)
                          .setEventCompleted(
                              eventId: event.id, completed: true);
                      if (context.mounted &&
                          !ref.read(calendarWriteControllerProvider).hasError) {
                        AppFeedback.success(context, 'Event completed ✓',
                            ref: ref);
                      }
                    },
                    onEdit: (event) async {
                      final input = await showEditEventDialog(
                        context,
                        selectedDay: selectedDay,
                        event: event,
                      );
                      if (input == null) {
                        return;
                      }
                      await ref
                          .read(calendarWriteControllerProvider.notifier)
                          .updateEvent(
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
                    },
                    onDelete: (event) async {
                      await ref
                          .read(calendarWriteControllerProvider.notifier)
                          .deleteEvent(event.id);
                      if (context.mounted &&
                          !ref.read(calendarWriteControllerProvider).hasError) {
                        AppFeedback.success(context, 'Event deleted', ref: ref);
                      }
                    },
                  );
                },
                loading: () => Column(
                  children: List.generate(3, (_) => AppSkeleton.card(context))
                      .expand((element) => [
                            element,
                            const SizedBox(height: AppSpacing.listGap),
                          ])
                      .toList(),
                ),
                error: (_, __) => ErrorMessage(
                  label: 'Unable to load events',
                  onRetry: () => ref.invalidate(dayEventsProvider),
                ),
              ),
            ),
          ),
        ],
      ),
    );
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
