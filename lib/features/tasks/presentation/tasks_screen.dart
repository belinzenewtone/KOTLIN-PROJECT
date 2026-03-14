import 'package:beltech/core/theme/app_colors.dart';
import 'package:beltech/core/theme/app_spacing.dart';
import 'package:beltech/core/widgets/app_capsule.dart';
import 'package:beltech/core/widgets/app_empty_state.dart';
import 'package:beltech/core/widgets/app_feedback.dart';
import 'package:beltech/core/widgets/app_search_bar.dart';
import 'package:beltech/core/widgets/app_skeleton.dart';
import 'package:beltech/core/widgets/error_message.dart';
import 'package:beltech/core/widgets/page_header.dart';
import 'package:beltech/core/widgets/page_shell.dart';
import 'package:beltech/features/search/domain/entities/global_search_result.dart';
import 'package:beltech/features/search/presentation/providers/global_search_providers.dart';
import 'package:beltech/features/tasks/domain/entities/task_item.dart';
import 'package:beltech/features/tasks/presentation/providers/tasks_providers.dart';
import 'package:beltech/features/tasks/presentation/widgets/task_item_card.dart';
import 'package:beltech/features/tasks/presentation/widgets/task_dialogs.dart';
import 'package:beltech/features/tasks/presentation/widgets/task_selection_bar.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class TasksScreen extends ConsumerStatefulWidget {
  const TasksScreen({super.key});

  @override
  ConsumerState<TasksScreen> createState() => _TasksScreenState();
}

class _TasksScreenState extends ConsumerState<TasksScreen> {
  bool _showSearch = false;
  bool _selectionMode = false;
  final Set<int> _selectedTaskIds = <int>{};
  final TextEditingController _searchController = TextEditingController();

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final tasksState = ref.watch(filteredTasksProvider);
    final allTasksState = ref.watch(tasksProvider);
    final allTasks = allTasksState.valueOrNull ?? const <TaskItem>[];
    if (allTasksState.hasValue) {
      _syncSelectionWithTasks(allTasks);
      _consumeSearchTarget(context, allTasks);
    }
    final selectedFilter = ref.watch(taskFilterProvider);
    final writeState = ref.watch(taskWriteControllerProvider);

    ref.listen<AsyncValue<void>>(taskWriteControllerProvider, (previous, next) {
      if (next.hasError) {
        AppFeedback.error(context, 'Task action failed. Please try again.',
            ref: ref);
      }
    });

    final countSubtitle = _selectionMode
        ? '${_selectedTaskIds.length} selected'
        : _buildCountSubtitle(allTasksState);

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
                if (!_selectionMode)
                  IconButton(
                    tooltip: 'Search',
                    onPressed: () {
                      setState(() {
                        _showSearch = !_showSearch;
                      });
                      if (!_showSearch) {
                        _searchController.clear();
                      }
                    },
                    icon: const Icon(Icons.search_rounded),
                  ),
                if (_selectionMode)
                  IconButton(
                    tooltip: allTasks.isEmpty
                        ? 'Select all'
                        : _selectedTaskIds.length == allTasks.length
                            ? 'Clear selection'
                            : 'Select all',
                    onPressed: writeState.isLoading || allTasks.isEmpty
                        ? null
                        : () => _toggleSelectAll(allTasks),
                    icon: Icon(
                      _selectedTaskIds.length == allTasks.length
                          ? Icons.deselect_rounded
                          : Icons.select_all_rounded,
                    ),
                  ),
                IconButton(
                  tooltip: _selectionMode
                      ? 'Exit multi-select'
                      : 'Select multiple tasks',
                  onPressed: writeState.isLoading
                      ? null
                      : () {
                          setState(() {
                            _selectionMode = !_selectionMode;
                            if (!_selectionMode) {
                              _selectedTaskIds.clear();
                            }
                          });
                        },
                  icon: Icon(_selectionMode
                      ? Icons.close_rounded
                      : Icons.checklist_rtl_rounded),
                ),
                IconButton(
                  tooltip: 'Add task',
                  onPressed: writeState.isLoading || _selectionMode
                      ? null
                      : () async {
                          final input = await showAddTaskDialog(context);
                          if (input == null) return;
                          await ref
                              .read(taskWriteControllerProvider.notifier)
                              .addTask(
                                title: input.title,
                                description: input.description,
                                dueDate: input.dueDate,
                                priority: input.priority,
                              );
                          if (context.mounted &&
                              !ref.read(taskWriteControllerProvider).hasError) {
                            AppFeedback.success(context, 'Task added',
                                ref: ref);
                          }
                        },
                  icon: const Icon(Icons.add_rounded),
                ),
              ],
            ),
          ),
          if (_showSearch) ...[
            AppSearchBar(
              controller: _searchController,
              hint: 'Search tasks...',
              onChanged: (_) => setState(() {}),
            ),
            const SizedBox(height: AppSpacing.sectionGap),
          ],
          if (_selectionMode) ...[
            TaskSelectionBar(
              selectedCount: _selectedTaskIds.length,
              isLoading: writeState.isLoading,
              onComplete: () => _completeSelected(context),
              onArchive: () => _archiveSelected(context),
              onDelete: () => _deleteSelected(context),
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
                      ref.read(taskFilterProvider.notifier).state = filter;
                    },
                    child: AppCapsule(
                      label: _filterLabel(filter),
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
                    selectionMode: _selectionMode,
                    selected: _selectedTaskIds.contains(tasks[index].id),
                    onSelectToggle: () => _toggleTaskSelection(tasks[index].id),
                    busy: writeState.isLoading,
                    onToggle: () async {
                      if (_selectionMode) {
                        _toggleTaskSelection(tasks[index].id);
                        return;
                      }
                      final isCompleting = !tasks[index].completed;
                      await ref
                          .read(taskWriteControllerProvider.notifier)
                          .toggleTask(
                            taskId: tasks[index].id,
                            completed: isCompleting,
                          );
                      if (context.mounted &&
                          !ref.read(taskWriteControllerProvider).hasError) {
                        AppFeedback.success(
                          context,
                          isCompleting
                              ? 'Task completed ✓'
                              : 'Task marked as pending',
                          ref: ref,
                        );
                      }
                    },
                    onEdit: () async {
                      await _editTask(context, tasks[index]);
                    },
                    onDelete: () async {
                      if (_selectionMode) {
                        _toggleTaskSelection(tasks[index].id);
                        return;
                      }
                      await ref
                          .read(taskWriteControllerProvider.notifier)
                          .deleteTask(
                            tasks[index].id,
                          );
                      if (context.mounted &&
                          !ref.read(taskWriteControllerProvider).hasError) {
                        AppFeedback.success(context, 'Task deleted', ref: ref);
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
                onRetry: () => ref.invalidate(filteredTasksProvider),
              ),
            ),
          ),
        ],
      ),
    );
  }

  String _filterLabel(TaskFilter filter) {
    return switch (filter) {
      TaskFilter.all => 'All',
      TaskFilter.pending => 'Pending',
      TaskFilter.completed => 'Completed',
    };
  }

  String _buildCountSubtitle(AsyncValue<List<TaskItem>> tasksState) {
    final tasks = tasksState.valueOrNull;
    if (tasks == null) {
      return 'Loading tasks...';
    }
    final pending = tasks.where((task) => !task.completed).length;
    final completed = tasks.where((task) => task.completed).length;
    return '$pending pending · $completed completed';
  }

  void _toggleTaskSelection(int taskId) {
    setState(() {
      if (!_selectionMode) {
        _selectionMode = true;
        _selectedTaskIds.add(taskId);
        return;
      }
      if (!_selectedTaskIds.remove(taskId)) {
        _selectedTaskIds.add(taskId);
      }
      if (_selectedTaskIds.isEmpty) {
        _selectionMode = false;
      }
    });
  }

  void _toggleSelectAll(List<TaskItem> tasks) {
    setState(() {
      if (_selectedTaskIds.length == tasks.length) {
        _selectedTaskIds.clear();
        _selectionMode = false;
      } else {
        _selectionMode = true;
        _selectedTaskIds
          ..clear()
          ..addAll(tasks.map((task) => task.id));
      }
    });
  }

  void _syncSelectionWithTasks(List<TaskItem> allTasks) {
    if (_selectedTaskIds.isEmpty) {
      return;
    }
    final ids = allTasks.map((task) => task.id).toSet();
    final hasStale = _selectedTaskIds.any((id) => !ids.contains(id));
    if (!hasStale) {
      return;
    }
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) {
        return;
      }
      setState(() {
        _selectedTaskIds.removeWhere((id) => !ids.contains(id));
        if (_selectedTaskIds.isEmpty) {
          _selectionMode = false;
        }
      });
    });
  }

  void _consumeSearchTarget(BuildContext context, List<TaskItem> allTasks) {
    final target = ref.read(globalSearchDeepLinkTargetProvider);
    if (target?.kind != GlobalSearchKind.task) {
      return;
    }

    ref.read(globalSearchDeepLinkTargetProvider.notifier).state = null;

    final recordId = target?.recordId;
    if (recordId == null) {
      return;
    }
    final task = allTasks.where((item) => item.id == recordId).firstOrNull;
    if (task == null) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (context.mounted) {
          AppFeedback.info(context, 'This task no longer exists.', ref: ref);
        }
      });
      return;
    }

    ref.read(taskFilterProvider.notifier).state = TaskFilter.all;
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      if (!context.mounted) {
        return;
      }
      setState(() {
        _selectionMode = false;
        _selectedTaskIds.clear();
      });
      await _editTask(context, task);
    });
  }

  Future<void> _editTask(BuildContext context, TaskItem task) async {
    final input = await showEditTaskDialog(context, task: task);
    if (input == null) {
      return;
    }
    await ref.read(taskWriteControllerProvider.notifier).updateTask(
          taskId: task.id,
          title: input.title,
          description: input.description,
          dueDate: input.dueDate,
          priority: input.priority,
        );
    if (context.mounted && !ref.read(taskWriteControllerProvider).hasError) {
      AppFeedback.success(context, 'Task updated', ref: ref);
    }
  }

  Future<void> _completeSelected(BuildContext context) async {
    final ids = _selectedTaskIds.toList(growable: false);
    if (ids.isEmpty) {
      return;
    }
    final count =
        await ref.read(taskWriteControllerProvider.notifier).completeTasks(ids);
    if (!context.mounted) {
      return;
    }
    if (!ref.read(taskWriteControllerProvider).hasError) {
      AppFeedback.success(
        context,
        count == 1 ? '1 task completed' : '$count tasks completed',
        ref: ref,
      );
      setState(() {
        _selectionMode = false;
        _selectedTaskIds.clear();
      });
    }
  }

  Future<void> _archiveSelected(BuildContext context) async {
    final ids = _selectedTaskIds.toList(growable: false);
    if (ids.isEmpty) {
      return;
    }
    final count =
        await ref.read(taskWriteControllerProvider.notifier).archiveTasks(ids);
    if (!context.mounted) {
      return;
    }
    if (!ref.read(taskWriteControllerProvider).hasError) {
      AppFeedback.success(
        context,
        count == 1
            ? '1 task archived to completed'
            : '$count tasks archived to completed',
        ref: ref,
      );
      setState(() {
        _selectionMode = false;
        _selectedTaskIds.clear();
      });
    }
  }

  Future<void> _deleteSelected(BuildContext context) async {
    final ids = _selectedTaskIds.toList(growable: false);
    if (ids.isEmpty) {
      return;
    }
    final confirmed = await showDialog<bool>(
          context: context,
          builder: (context) => AlertDialog(
            title: const Text('Delete selected tasks?'),
            content: Text(
              ids.length == 1
                  ? 'This action cannot be undone.'
                  : 'This will permanently delete ${ids.length} tasks.',
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.of(context).pop(false),
                child: const Text('Cancel'),
              ),
              FilledButton(
                onPressed: () => Navigator.of(context).pop(true),
                child: const Text('Delete'),
              ),
            ],
          ),
        ) ??
        false;
    if (!confirmed) {
      return;
    }
    final count =
        await ref.read(taskWriteControllerProvider.notifier).deleteTasks(ids);
    if (!context.mounted) {
      return;
    }
    if (!ref.read(taskWriteControllerProvider).hasError) {
      AppFeedback.success(
        context,
        count == 1 ? '1 task deleted' : '$count tasks deleted',
        ref: ref,
      );
      setState(() {
        _selectionMode = false;
        _selectedTaskIds.clear();
      });
    }
  }
}
