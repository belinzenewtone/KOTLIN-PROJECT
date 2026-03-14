import 'package:beltech/core/theme/app_colors.dart';
import 'package:flutter/material.dart';

class AssistantPromptGrid extends StatelessWidget {
  const AssistantPromptGrid({
    required this.prompts,
    required this.onPromptTap,
    super.key,
  });

  final List<String> prompts;
  final Future<void> Function(String) onPromptTap;

  @override
  Widget build(BuildContext context) {
    if (prompts.isEmpty) {
      return const SizedBox.shrink();
    }
    return SizedBox(
      height: 34,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        itemCount: prompts.length,
        separatorBuilder: (_, __) => const SizedBox(width: 8),
        itemBuilder: (context, index) {
          final prompt = prompts[index];
          return ActionChip(
            label: Text(
              prompt,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
            onPressed: () => onPromptTap(prompt),
            side: BorderSide(
              color: AppColors.accent.withValues(alpha: 0.35),
            ),
            backgroundColor: AppColors.accentSoft.withValues(alpha: 0.65),
            labelStyle: Theme.of(context).textTheme.bodySmall?.copyWith(
                  fontWeight: FontWeight.w600,
                  color: AppColors.textSecondary,
                ),
            visualDensity: VisualDensity.compact,
            materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
          );
        },
      ),
    );
  }
}
