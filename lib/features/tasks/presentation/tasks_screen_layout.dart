part of 'tasks_screen.dart';

class _TasksLayout extends StatelessWidget {
  const _TasksLayout({
    required this.state,
    required this.tasksState,
    required this.allTasks,
    required this.selectedFilter,
    required this.writeState,
    required this.countSubtitle,
  });

  final _TasksScreenState state;
  final AsyncValue<List<TaskItem>> tasksState;
  final List<TaskItem> allTasks;
  final TaskFilter selectedFilter;
  final AsyncValue<void> writeState;
  final String countSubtitle;

  @override
  Widget build(BuildContext context) {
    return PageShell(
      scrollable: false,
      glowColor: AppColors.glowViolet,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          PageHeader(
            eyebrow: 'FOCUS',
            title: 'Tasks',
            subtitle: countSubtitle,
            action: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                if (!state._selectionMode)
                  IconButton(
                    tooltip: 'Search',
                    onPressed: state._toggleSearch,
                    icon: const Icon(Icons.search_rounded),
                  ),
                if (state._selectionMode)
                  IconButton(
                    tooltip: allTasks.isEmpty
                        ? 'Select all'
                        : state._selectedTaskIds.length == allTasks.length
                            ? 'Clear selection'
                            : 'Select all',
                    onPressed: writeState.isLoading || allTasks.isEmpty
                        ? null
                        : () => state._toggleSelectAll(allTasks),
                    icon: Icon(
                      state._selectedTaskIds.length == allTasks.length
                          ? Icons.deselect_rounded
                          : Icons.select_all_rounded,
                    ),
                  ),
                IconButton(
                  tooltip: state._selectionMode
                      ? 'Exit multi-select'
                      : 'Select multiple tasks',
                  onPressed: writeState.isLoading
                      ? null
                      : state._toggleSelectionMode,
                  icon: Icon(
                    state._selectionMode
                        ? Icons.close_rounded
                        : Icons.checklist_rtl_rounded,
                  ),
                ),
                IconButton(
                  tooltip: 'Add task',
                  onPressed: writeState.isLoading || state._selectionMode
                      ? null
                      : () async {
                          final input = await showAddTaskDialog(context);
                          if (input == null) {
                            return;
                          }
                          await state.ref
                              .read(taskWriteControllerProvider.notifier)
                              .addTask(
                                title: input.title,
                                description: input.description,
                                dueDate: input.dueDate,
                                priority: input.priority,
                              );
                          if (context.mounted &&
                              !state
                                  .ref
                                  .read(taskWriteControllerProvider)
                                  .hasError) {
                            AppFeedback.success(context, 'Task added', ref: state.ref);
                          }
                        },
                  icon: const Icon(Icons.add_rounded),
                ),
              ],
            ),
          ),
          if (state._showSearch) ...[
            AppSearchBar(
              controller: state._searchController,
              hint: 'Search tasks...',
              onChanged: (_) => state._refreshSearchResults(),
            ),
            const SizedBox(height: AppSpacing.sectionGap),
          ],
          if (state._selectionMode) ...[
            TaskSelectionBar(
              selectedCount: state._selectedTaskIds.length,
              isLoading: writeState.isLoading,
              onComplete: () => state._completeSelected(context),
              onArchive: () => state._archiveSelected(context),
              onDelete: () => state._deleteSelected(context),
            ),
            const SizedBox(height: AppSpacing.sectionGap),
          ],
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: TaskFilter.values
                .map(
                  (filter) => GestureDetector(
                    onTap: () {
                      state.ref.read(taskFilterProvider.notifier).state = filter;
                    },
                    child: AppCapsule(
                      label: state._filterLabel(filter),
                      color: selectedFilter == filter
                          ? AppColors.accent
                          : AppColors.textMuted,
                      variant: selectedFilter == filter
                          ? AppCapsuleVariant.solid
                          : AppCapsuleVariant.subtle,
                      size: AppCapsuleSize.sm,
                    ),
                  ),
                )
                .toList(),
          ),
          const SizedBox(height: AppSpacing.sectionGap),
          Expanded(
            child: tasksState.when(
              data: (tasks) {
                if (tasks.isEmpty) {
                  return AppEmptyState(
                    icon: Icons.task_alt_rounded,
                    title: 'No tasks here',
                    subtitle: 'Tap + to add your first task',
                  );
                }
                return ListView.separated(
                  itemBuilder: (_, index) => TaskItemCard(
                    task: tasks[index],
                    selectionMode: state._selectionMode,
                    selected: state._selectedTaskIds.contains(tasks[index].id),
                    onSelectToggle: () =>
                        state._toggleTaskSelection(tasks[index].id),
                    busy: writeState.isLoading,
                    onToggle: () async {
                      if (state._selectionMode) {
                        state._toggleTaskSelection(tasks[index].id);
                        return;
                      }
                      final isCompleting = !tasks[index].completed;
                      await state.ref
                          .read(taskWriteControllerProvider.notifier)
                          .toggleTask(
                            taskId: tasks[index].id,
                            completed: isCompleting,
                          );
                      if (context.mounted &&
                          !state.ref.read(taskWriteControllerProvider).hasError) {
                        AppFeedback.success(
                          context,
                          isCompleting
                              ? 'Task completed ✓'
                              : 'Task marked as pending',
                          ref: state.ref,
                        );
                      }
                    },
                    onEdit: () async {
                      await state._editTask(context, tasks[index]);
                    },
                    onDelete: () async {
                      if (state._selectionMode) {
                        state._toggleTaskSelection(tasks[index].id);
                        return;
                      }
                      await state
                          .ref
                          .read(taskWriteControllerProvider.notifier)
                          .deleteTask(tasks[index].id);
                      if (context.mounted &&
                          !state.ref.read(taskWriteControllerProvider).hasError) {
                        AppFeedback.success(context, 'Task deleted', ref: state.ref);
                      }
                    },
                  ),
                  separatorBuilder: (_, __) =>
                      const SizedBox(height: AppSpacing.listGap),
                  itemCount: tasks.length,
                );
              },
              loading: () => Column(
                children: List.generate(5, (_) => const TaskCardSkeleton())
                    .expand((element) => [
                          element,
                          const SizedBox(height: AppSpacing.listGap),
                        ])
                    .toList(),
              ),
              error: (_, __) => ErrorMessage(
                label: 'Unable to load tasks',
                onRetry: () => state.ref.invalidate(filteredTasksProvider),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
