import 'package:beltech/core/theme/app_colors.dart';
import 'package:beltech/features/calendar/domain/entities/calendar_event.dart';
import 'package:flutter/material.dart';

class CalendarMonthGrid extends StatelessWidget {
  const CalendarMonthGrid({
    super.key,
    required this.visibleMonth,
    required this.selectedDay,
    required this.eventTypes,
    required this.maxWidth,
    required this.onSelect,
  });

  final DateTime visibleMonth;
  final DateTime selectedDay;
  final Map<int, CalendarEventType> eventTypes;
  final double maxWidth;
  final ValueChanged<DateTime> onSelect;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final totalDays =
        DateTime(visibleMonth.year, visibleMonth.month + 1, 0).day;
    final leadingBlanks =
        DateTime(visibleMonth.year, visibleMonth.month, 1).weekday - 1;
    final totalItems = ((leadingBlanks + totalDays + 6) ~/ 7) * 7;
    final today = DateTime.now();

    return Center(
      child: ConstrainedBox(
        constraints: BoxConstraints(maxWidth: maxWidth),
        child: GridView.builder(
          itemCount: totalItems,
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: 7,
            mainAxisExtent: 42,
          ),
          itemBuilder: (context, index) {
            final day = index - leadingBlanks + 1;
            if (day < 1 || day > totalDays) {
              return const SizedBox.shrink();
            }

            final current =
                DateTime(visibleMonth.year, visibleMonth.month, day);
            final isSelected = selectedDay.year == current.year &&
                selectedDay.month == current.month &&
                selectedDay.day == current.day;
            final isToday = today.year == current.year &&
                today.month == current.month &&
                today.day == current.day;
            final eventType = eventTypes[day];
            final hasEvents = eventType != null;
            final dotColor = isSelected
                ? AppColors.textPrimary
                : (hasEvents
                    ? _eventTypeColor(eventType)
                    : AppColors.textSecondary);

            return Center(
              child: InkWell(
                customBorder: const CircleBorder(),
                onTap: () => onSelect(current),
                child: AnimatedContainer(
                  duration: const Duration(milliseconds: 240),
                  width: 38,
                  height: 38,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: isSelected ? AppColors.accent : Colors.transparent,
                  ),
                  child: Stack(
                    alignment: Alignment.center,
                    children: [
                      Text(
                        '$day',
                        style: textTheme.bodyLarge?.copyWith(
                          color: isSelected
                              ? AppColors.textPrimary
                              : AppColors.textSecondary,
                        ),
                      ),
                      if (isToday || hasEvents)
                        Positioned(
                          bottom: 6,
                          child: Container(
                            width: 4,
                            height: 4,
                            decoration: BoxDecoration(
                              color: dotColor,
                              shape: BoxShape.circle,
                            ),
                          ),
                        ),
                    ],
                  ),
                ),
              ),
            );
          },
        ),
      ),
    );
  }
}

Color _eventTypeColor(CalendarEventType type) {
  return switch (type) {
    CalendarEventType.work => AppColors.accent,
    CalendarEventType.personal => AppColors.violet,
    CalendarEventType.finance => AppColors.teal,
    CalendarEventType.health => AppColors.warning,
    CalendarEventType.general => AppColors.slate,
  };
}
