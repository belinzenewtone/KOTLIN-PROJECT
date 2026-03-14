import 'package:beltech/core/widgets/glass_card.dart';
import 'package:flutter/material.dart';

/// Appears when the user is in multi-select mode.
/// Shows count + bulk action buttons (complete, archive, delete).
class TaskSelectionBar extends StatelessWidget {
  const TaskSelectionBar({
    super.key,
    required this.selectedCount,
    required this.isLoading,
    required this.onComplete,
    required this.onArchive,
    required this.onDelete,
  });

  final int selectedCount;
  final bool isLoading;
  final VoidCallback onComplete;
  final VoidCallback onArchive;
  final VoidCallback onDelete;

  @override
  Widget build(BuildContext context) {
    final disabled = isLoading || selectedCount == 0;
    return GlassCard(
      tone: GlassCardTone.muted,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '$selectedCount selected',
            style: Theme.of(context).textTheme.titleSmall,
          ),
          const SizedBox(height: 10),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              FilledButton.icon(
                onPressed: disabled ? null : onComplete,
                icon: const Icon(Icons.done_all_rounded),
                label: const Text('Complete'),
              ),
              FilledButton.icon(
                onPressed: disabled ? null : onArchive,
                icon: const Icon(Icons.archive_outlined),
                label: const Text('Archive'),
              ),
              OutlinedButton.icon(
                onPressed: disabled ? null : onDelete,
                icon: const Icon(Icons.delete_outline_rounded),
                label: const Text('Delete'),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
