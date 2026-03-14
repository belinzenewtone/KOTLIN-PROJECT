import 'package:beltech/core/theme/app_colors.dart';
import 'package:beltech/core/theme/app_spacing.dart';
import 'package:beltech/core/theme/app_typography.dart';
import 'package:beltech/core/utils/currency_formatter.dart';
import 'package:beltech/core/widgets/app_capsule.dart';
import 'package:beltech/core/widgets/glass_card.dart';
import 'package:beltech/features/home/domain/entities/home_overview.dart';
import 'package:flutter/material.dart';

/// Two-tile strip: Today spend + This Week spend.
class HomeSpendSnapshotStrip extends StatelessWidget {
  const HomeSpendSnapshotStrip({super.key, required this.overview});
  final HomeOverview overview;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: GlassCard(
            tone: GlassCardTone.accent,
            accentColor: AppColors.accent,
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Today', style: AppTypography.bodySm(context)),
                const SizedBox(height: 4),
                FittedBox(
                  fit: BoxFit.scaleDown,
                  alignment: Alignment.centerLeft,
                  child: Text(
                    CurrencyFormatter.money(overview.todayKes),
                    style: AppTypography.amount(context),
                    maxLines: 1,
                    softWrap: false,
                  ),
                ),
              ],
            ),
          ),
        ),
        const SizedBox(width: AppSpacing.cardGap),
        Expanded(
          child: GlassCard(
            tone: GlassCardTone.accent,
            accentColor: AppColors.violet,
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('This Week', style: AppTypography.bodySm(context)),
                const SizedBox(height: 4),
                FittedBox(
                  fit: BoxFit.scaleDown,
                  alignment: Alignment.centerLeft,
                  child: Text(
                    CurrencyFormatter.money(overview.weekKes),
                    style: AppTypography.amount(context),
                    maxLines: 1,
                    softWrap: false,
                  ),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}

/// Teaser card prompting the user to open the AI assistant.
class HomeAiInsightCard extends StatelessWidget {
  const HomeAiInsightCard({super.key});

  @override
  Widget build(BuildContext context) {
    return GlassCard(
      tone: GlassCardTone.muted,
      padding: const EdgeInsets.all(14),
      child: Row(
        children: [
          Container(
            width: 36,
            height: 36,
            decoration: BoxDecoration(
              color: AppColors.violet.withValues(alpha: 0.18),
              shape: BoxShape.circle,
            ),
            child: const Icon(
              Icons.auto_awesome_rounded,
              color: AppColors.violet,
              size: 18,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Expanded(
                      child: Text(
                        'AI Insight',
                        style: AppTypography.cardTitle(context),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                    const SizedBox(width: 6),
                    AppCapsule(
                      label: 'NEW',
                      color: AppColors.violet,
                      variant: AppCapsuleVariant.subtle,
                      size: AppCapsuleSize.sm,
                    ),
                  ],
                ),
                const SizedBox(height: 3),
                Text(
                  'Ask your assistant for spending tips, task priorities, or weekly summaries.',
                  style: AppTypography.bodySm(context),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            ),
          ),
          const Icon(
            Icons.chevron_right_rounded,
            color: AppColors.textMuted,
            size: 20,
          ),
        ],
      ),
    );
  }
}

/// Task completion progress bar card.
class HomeProductivityCard extends StatelessWidget {
  const HomeProductivityCard({super.key, required this.overview});
  final HomeOverview overview;

  @override
  Widget build(BuildContext context) {
    final total = overview.completedCount + overview.pendingCount;
    final progress = total == 0 ? 0.0 : overview.completedCount / total;
    final brightness = Theme.of(context).brightness;

    return GlassCard(
      child: Row(
        children: [
          Container(
            width: 44,
            height: 44,
            decoration: BoxDecoration(
              color: AppColors.teal.withValues(alpha: 0.18),
              shape: BoxShape.circle,
            ),
            child: const Icon(Icons.check_circle_rounded,
                color: AppColors.teal, size: 22),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Productivity', style: AppTypography.cardTitle(context)),
                const SizedBox(height: 4),
                ClipRRect(
                  borderRadius: BorderRadius.circular(4),
                  child: LinearProgressIndicator(
                    value: progress,
                    minHeight: 4,
                    backgroundColor:
                        AppColors.borderFor(brightness).withValues(alpha: 0.3),
                    valueColor:
                        const AlwaysStoppedAnimation<Color>(AppColors.teal),
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  '${overview.completedCount} done · ${overview.pendingCount} pending',
                  style: AppTypography.bodySm(context),
                  maxLines: 1,
                  softWrap: false,
                  overflow: TextOverflow.fade,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

/// Weekly spend vs today spend balance summary card.
class HomeBalanceCard extends StatelessWidget {
  const HomeBalanceCard({super.key, required this.overview});
  final HomeOverview overview;

  @override
  Widget build(BuildContext context) {
    return GlassCard(
      tone: GlassCardTone.accent,
      accentColor: AppColors.accent,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('Spend', style: AppTypography.bodySm(context)),
                    const SizedBox(height: 4),
                    FittedBox(
                      fit: BoxFit.scaleDown,
                      alignment: Alignment.centerLeft,
                      child: Text(
                        CurrencyFormatter.money(overview.weekKes),
                        style: AppTypography.amountLg(context),
                        maxLines: 1,
                        softWrap: false,
                      ),
                    ),
                  ],
                ),
              ),
              AppCapsule(
                label: 'Current',
                color: AppColors.success,
                variant: AppCapsuleVariant.subtle,
                size: AppCapsuleSize.sm,
              ),
            ],
          ),
          const SizedBox(height: 14),
          Row(
            children: [
              _HomeBalanceStat(
                label: 'This Week',
                value: CurrencyFormatter.money(overview.weekKes),
                color: AppColors.success,
              ),
              const SizedBox(width: AppSpacing.cardGap),
              _HomeBalanceStat(
                label: 'Today',
                value: CurrencyFormatter.money(overview.todayKes),
                color: AppColors.danger,
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _HomeBalanceStat extends StatelessWidget {
  const _HomeBalanceStat(
      {required this.label, required this.value, required this.color});
  final String label;
  final String value;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                  width: 8,
                  height: 8,
                  decoration:
                      BoxDecoration(color: color, shape: BoxShape.circle)),
              const SizedBox(width: 5),
              Expanded(
                child: Text(
                  label,
                  style: AppTypography.bodySm(context),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ),
            ],
          ),
          const SizedBox(height: 2),
          Text(
            value,
            style: AppTypography.bodyMd(context)
                .copyWith(fontWeight: FontWeight.w600, color: color),
            maxLines: 1,
            softWrap: false,
            overflow: TextOverflow.fade,
          ),
        ],
      ),
    );
  }
}

/// Single transaction row used in the dashboard recent transactions list.
class HomeDashboardTransactionTile extends StatelessWidget {
  const HomeDashboardTransactionTile({super.key, required this.tx});
  final HomeTransaction tx;

  @override
  Widget build(BuildContext context) {
    final catColor = AppColors.categoryColorFor(tx.category);
    return GlassCard(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      child: Row(
        children: [
          Container(
            width: 40,
            height: 40,
            decoration: BoxDecoration(
              color: catColor.withValues(alpha: 0.18),
              shape: BoxShape.circle,
            ),
            child: Icon(Icons.payments_outlined, color: catColor, size: 20),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(tx.title,
                    style: AppTypography.cardTitle(context),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis),
                Text(
                  tx.category,
                  style: AppTypography.bodySm(context),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            ),
          ),
          Text(
            CurrencyFormatter.money(tx.amountKes),
            style: AppTypography.bodyMd(context)
                .copyWith(fontWeight: FontWeight.w600),
            maxLines: 1,
            softWrap: false,
            overflow: TextOverflow.fade,
          ),
        ],
      ),
    );
  }
}
