import 'package:beltech/core/theme/app_colors.dart';
import 'package:beltech/core/theme/app_spacing.dart';
import 'package:beltech/core/theme/app_typography.dart';
import 'package:beltech/core/widgets/glass_card.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

class SettingsToolsCard extends StatelessWidget {
  const SettingsToolsCard({super.key});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        _SettingsToolTile(
          icon: Icons.savings_outlined,
          title: 'Budgets',
          subtitle: 'Set monthly limits per category',
          onTap: () => context.pushNamed('budget'),
        ),
        const SizedBox(height: AppSpacing.listGap),
        _SettingsToolTile(
          icon: Icons.account_balance_wallet_outlined,
          title: 'Income',
          subtitle: 'Track incoming cashflow',
          onTap: () => context.pushNamed('income'),
        ),
        const SizedBox(height: AppSpacing.listGap),
        _SettingsToolTile(
          icon: Icons.autorenew,
          title: 'Recurring Items',
          subtitle: 'Automate repeating records',
          onTap: () => context.pushNamed('recurring'),
        ),
        const SizedBox(height: AppSpacing.listGap),
        _SettingsToolTile(
          icon: Icons.search,
          title: 'Global Search',
          subtitle: 'Search expenses, tasks, events, and more',
          onTap: () => context.pushNamed('search'),
        ),
        const SizedBox(height: AppSpacing.listGap),
        _SettingsToolTile(
          icon: Icons.file_download_outlined,
          title: 'Export CSV',
          subtitle: 'Export your data for backup',
          onTap: () => context.pushNamed('export'),
        ),
        const SizedBox(height: AppSpacing.listGap),
        _SettingsToolTile(
          icon: Icons.query_stats,
          title: 'Analytics',
          subtitle: 'View trends and performance metrics',
          onTap: () => context.pushNamed('analytics'),
        ),
      ],
    );
  }
}

class _SettingsToolTile extends StatelessWidget {
  const _SettingsToolTile({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.onTap,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return GlassCard(
      tone: GlassCardTone.muted,
      onTap: onTap,
      child: Row(
        children: [
          Icon(icon, color: AppColors.accent),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: AppTypography.cardTitle(context),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 4),
                Text(
                  subtitle,
                  style: AppTypography.bodySm(context),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            ),
          ),
          Icon(Icons.chevron_right, color: AppColors.textMuted),
        ],
      ),
    );
  }
}
