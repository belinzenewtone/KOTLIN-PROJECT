part of 'calendar_screen.dart';

class _CalendarEventsPane extends StatelessWidget {
  const _CalendarEventsPane({
    required this.state,
    required this.eventsState,
    required this.selectedDay,
    required this.writeState,
    required this.eventsPaneHeight,
  });

  final _CalendarScreenState state;
  final AsyncValue<List<CalendarEvent>> eventsState;
  final DateTime selectedDay;
  final AsyncValue<void> writeState;
  final double eventsPaneHeight;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: eventsPaneHeight,
      child: AbsorbPointer(
        absorbing: state._swiping,
        child: eventsState.when(
          data: (events) {
            state._consumeSearchTarget(context, state.ref, selectedDay, events);
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
                await state
                    .ref
                    .read(calendarWriteControllerProvider.notifier)
                    .setEventCompleted(eventId: event.id, completed: true);
                if (context.mounted &&
                    !state.ref.read(calendarWriteControllerProvider).hasError) {
                  AppFeedback.success(context, 'Event completed ✓', ref: state.ref);
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
                await state
                    .ref
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
                    !state.ref.read(calendarWriteControllerProvider).hasError) {
                  AppFeedback.success(context, 'Event updated', ref: state.ref);
                }
              },
              onDelete: (event) async {
                await state
                    .ref
                    .read(calendarWriteControllerProvider.notifier)
                    .deleteEvent(event.id);
                if (context.mounted &&
                    !state.ref.read(calendarWriteControllerProvider).hasError) {
                  AppFeedback.success(context, 'Event deleted', ref: state.ref);
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
            onRetry: () => state.ref.invalidate(dayEventsProvider),
          ),
        ),
      ),
    );
  }
}
