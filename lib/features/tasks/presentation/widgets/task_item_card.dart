import 'package:beltech/core/theme/app_colors.dart';
import 'package:beltech/core/theme/app_motion.dart';
import 'package:beltech/core/theme/app_radius.dart';
import 'package:beltech/core/theme/app_spacing.dart';
import 'package:beltech/core/widgets/app_capsule.dart';
import 'package:beltech/core/widgets/glass_card.dart';
import 'package:beltech/features/tasks/domain/entities/task_item.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class TaskItemCard extends StatelessWidget {
  const TaskItemCard({
    super.key,
    required this.task,
    required this.selectionMode,
    required this.selected,
    required this.onSelectToggle,
    required this.onToggle,
    required this.busy,
    required this.onEdit,
    required this.onDelete,
  });

  final TaskItem task;
  final bool selectionMode;
  final bool selected;
  final VoidCallback onSelectToggle;
  final Future<void> Function() onToggle;
  final bool busy;
  final Future<void> Function() onEdit;
  final Future<void> Function() onDelete;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final brightness = Theme.of(context).brightness;
    final secondaryText = AppColors.textSecondaryFor(brightness);
    final swipeDuration = AppMotion.swipe(context);
    final resizeDuration = AppMotion.resize(context);
    final priorityColor = _priorityColor(task.priority);
    final isOverdue = !task.completed &&
        task.dueDate != null &&
        task.dueDate!.isBefore(DateTime.now());

    final countdownBadge = _buildCountdownBadge(task);

    return Dismissible(
      key: ValueKey('task-${task.id}'),
      direction: busy || selectionMode
          ? DismissDirection.none
          : DismissDirection.horizontal,
      movementDuration: swipeDuration,
      resizeDuration: resizeDuration,
      dismissThresholds: const {
        DismissDirection.startToEnd: 0.4,
        DismissDirection.endToStart: 0.4,
      },
      confirmDismiss: (direction) async {
        if (direction == DismissDirection.startToEnd) {
          await onToggle();
          return false;
        }
        if (direction == DismissDirection.endToStart) {
          await onDelete();
          return false;
        }
        return false;
      },
      background: const _SwipeBackground(
        color: AppColors.successMuted,
        icon: Icons.check_circle_outline,
        alignment: Alignment.centerLeft,
      ),
      secondaryBackground: const _SwipeBackground(
        color: AppColors.dangerMuted,
        icon: Icons.delete_outline,
        alignment: Alignment.centerRight,
      ),
      child: GlassCard(
        tone: selectionMode && selected
            ? GlassCardTone.accent
            : GlassCardTone.standard,
        accentColor: selectionMode && selected ? AppColors.accent : null,
        child: InkWell(
          borderRadius: BorderRadius.circular(20),
          onTap: selectionMode ? onSelectToggle : null,
          onLongPress: busy
              ? null
              : () {
                  HapticFeedback.lightImpact();
                  onSelectToggle();
                },
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: 4,
                height: 76,
                margin: const EdgeInsets.only(top: 4),
                decoration: BoxDecoration(
                  color: priorityColor,
                  borderRadius: BorderRadius.circular(AppRadius.md),
                ),
              ),
              const SizedBox(width: 10),
              IconButton(
                onPressed: busy
                    ? null
                    : () {
                        HapticFeedback.lightImpact();
                        if (selectionMode) {
                          onSelectToggle();
                          return;
                        }
                        onToggle();
                      },
                icon: Icon(
                  selectionMode
                      ? (selected
                          ? Icons.check_circle_rounded
                          : Icons.radio_button_unchecked_rounded)
                      : (task.completed
                          ? Icons.check_circle
                          : Icons.radio_button_unchecked),
                  color: selectionMode
                      ? (selected ? AppColors.accent : secondaryText)
                      : (task.completed ? AppColors.success : secondaryText),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      task.title,
                      style: textTheme.bodyLarge?.copyWith(
                        decoration:
                            task.completed ? TextDecoration.lineThrough : null,
                      ),
                    ),
                    if (task.description != null &&
                        task.description!.isNotEmpty)
                      Padding(
                        padding: const EdgeInsets.only(top: 2),
                        child: Text(
                          task.description!,
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                          style: textTheme.bodyMedium?.copyWith(
                            color: secondaryText,
                          ),
                        ),
                      ),
                    const SizedBox(height: 6),
                    Row(
                      children: [
                        AppCapsule(
                          label: _priorityLabel(task.priority),
                          color: priorityColor,
                          variant: AppCapsuleVariant.subtle,
                          size: AppCapsuleSize.sm,
                        ),
                        const SizedBox(width: AppSpacing.listGap),
                        if (countdownBadge != null)
                          Expanded(child: countdownBadge)
                        else
                          Expanded(
                            child: Text(
                              task.completed
                                  ? 'Completed'
                                  : task.dueDate == null
                                      ? 'No due date'
                                      : 'Due on ${_formatDate(task.dueDate!)}',
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: textTheme.bodySmall?.copyWith(
                                color: task.completed
                                    ? AppColors.success
                                    : (isOverdue
                                        ? AppColors.danger
                                        : secondaryText),
                              ),
                            ),
                          ),
                      ],
                    ),
                  ],
                ),
              ),
              if (!selectionMode)
                IconButton(
                  onPressed: busy
                      ? null
                      : () {
                          onEdit();
                        },
                  icon: const Icon(Icons.edit_outlined),
                ),
            ],
          ),
        ),
      ),
    );
  }

  Widget? _buildCountdownBadge(TaskItem task) {
    if (task.completed || task.dueDate == null) {
      return null;
    }

    final now = DateTime.now();
    final due = task.dueDate!;
    final difference = due.difference(now);

    if (difference.isNegative) {
      // Overdue
      final days = (-difference.inDays).abs();
      return AppCapsule(
        label: 'Overdue ${days}d',
        color: AppColors.danger,
        variant: AppCapsuleVariant.solid,
        size: AppCapsuleSize.sm,
      );
    } else if (difference.inDays == 0) {
      // Due today
      return AppCapsule(
        label: 'Due today',
        color: AppColors.warning,
        variant: AppCapsuleVariant.subtle,
        size: AppCapsuleSize.sm,
      );
    } else if (difference.inHours < 3 && difference.inHours > 0) {
      // Due in less than 3 hours
      final hours = difference.inHours;
      return AppCapsule(
        label: 'Due in ${hours}h',
        color: AppColors.warning,
        variant: AppCapsuleVariant.subtle,
        size: AppCapsuleSize.sm,
      );
    } else if (difference.inDays == 1) {
      // Due tomorrow
      return AppCapsule(
        label: 'Due tomorrow',
        color: AppColors.accent,
        variant: AppCapsuleVariant.subtle,
        size: AppCapsuleSize.sm,
      );
    }

    return null;
  }

  String _formatDate(DateTime date) {
    final now = DateTime.now();
    final tomorrow = now.add(const Duration(days: 1));

    if (date.year == now.year &&
        date.month == now.month &&
        date.day == now.day) {
      return 'Today';
    }
    if (date.year == tomorrow.year &&
        date.month == tomorrow.month &&
        date.day == tomorrow.day) {
      return 'Tomorrow';
    }

    return '${date.month}/${date.day}/${date.year}';
  }

  String _priorityLabel(TaskPriority priority) {
    return switch (priority) {
      TaskPriority.high => 'Urgent',
      TaskPriority.medium => 'Important',
      TaskPriority.low => 'Neutral',
    };
  }
}

Color _priorityColor(TaskPriority priority) {
  return switch (priority) {
    TaskPriority.high => AppColors.danger,
    TaskPriority.medium => AppColors.warning,
    TaskPriority.low => AppColors.accent,
  };
}

class _SwipeBackground extends StatelessWidget {
  const _SwipeBackground({
    required this.color,
    required this.icon,
    required this.alignment,
  });

  final Color color;
  final IconData icon;
  final Alignment alignment;

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(22),
      ),
      padding: const EdgeInsets.symmetric(horizontal: 24),
      alignment: alignment,
      child: Icon(icon, color: Colors.white, size: 30),
    );
  }
}
