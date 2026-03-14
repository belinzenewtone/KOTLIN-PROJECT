import 'package:beltech/core/theme/app_colors.dart';
import 'package:beltech/core/theme/app_spacing.dart';
import 'package:beltech/core/theme/app_typography.dart';
import 'package:beltech/core/navigation/shell_providers.dart';
import 'package:beltech/core/widgets/glass_card.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

class HomeQuickActionsGrid extends StatelessWidget {
  const HomeQuickActionsGrid({super.key});

  @override
  Widget build(BuildContext context) {
    final actions = [
      _HomeQuickAction(
        icon: Icons.auto_awesome_rounded,
        label: 'Assistant',
        color: AppColors.violet,
        tabIndex: 4,
      ),
      _HomeQuickAction(
        icon: Icons.add_task_rounded,
        label: 'Add Task',
        color: AppColors.teal,
        tabIndex: 3,
      ),
      _HomeQuickAction(
        icon: Icons.account_balance_wallet_rounded,
        label: 'Finance',
        color: AppColors.accent,
        tabIndex: 2,
      ),
      _HomeQuickAction(
        icon: Icons.search_rounded,
        label: 'Search',
        color: AppColors.slate,
        route: '/search',
      ),
      _HomeQuickAction(
        icon: Icons.bar_chart_rounded,
        label: 'Analytics',
        color: AppColors.warning,
        route: '/analytics',
      ),
      _HomeQuickAction(
        icon: Icons.date_range_rounded,
        label: 'Week Review',
        color: AppColors.success,
        route: '/week-review',
      ),
    ];

    return LayoutBuilder(
      builder: (context, constraints) {
        final compact = constraints.maxWidth < 360;
        final crossAxisCount = compact ? 2 : 3;
        return GridView.builder(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: crossAxisCount,
            crossAxisSpacing: AppSpacing.cardGap,
            mainAxisSpacing: AppSpacing.cardGap,
            childAspectRatio: compact ? 1.25 : 1.05,
          ),
          itemCount: actions.length,
          itemBuilder: (context, index) =>
              _HomeQuickActionTile(action: actions[index]),
        );
      },
    );
  }
}

class _HomeQuickAction {
  const _HomeQuickAction({
    required this.icon,
    required this.label,
    required this.color,
    this.route,
    this.tabIndex,
  });

  final IconData icon;
  final String label;
  final Color color;
  final String? route;
  final int? tabIndex;
}

class _HomeQuickActionTile extends ConsumerWidget {
  const _HomeQuickActionTile({required this.action});
  final _HomeQuickAction action;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return GlassCard(
      tone: GlassCardTone.muted,
      padding: const EdgeInsets.symmetric(vertical: 14, horizontal: 10),
      onTap: () {
        HapticFeedback.lightImpact();
        if (action.tabIndex != null) {
          ref.read(shellTabIndexProvider.notifier).state = action.tabIndex!;
          return;
        }
        if (action.route != null) {
          context.push(action.route!);
        }
      },
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Container(
            width: 40,
            height: 40,
            decoration: BoxDecoration(
              color: action.color.withValues(alpha: 0.18),
              shape: BoxShape.circle,
            ),
            child: Icon(action.icon, color: action.color, size: 20),
          ),
          const SizedBox(height: 8),
          Text(
            action.label,
            style: AppTypography.bodySm(context).copyWith(
              fontWeight: FontWeight.w600,
              color: AppColors.textSecondary,
            ),
            textAlign: TextAlign.center,
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
          ),
        ],
      ),
    );
  }
}
