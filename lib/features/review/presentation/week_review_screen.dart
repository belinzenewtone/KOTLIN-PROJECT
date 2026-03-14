import 'package:beltech/core/theme/app_colors.dart';
import 'package:beltech/core/theme/app_spacing.dart';
import 'package:beltech/core/theme/app_typography.dart';
import 'package:beltech/core/utils/currency_formatter.dart';
import 'package:beltech/core/widgets/app_empty_state.dart';
import 'package:beltech/core/widgets/glass_card.dart';
import 'package:beltech/core/widgets/loading_indicator.dart';
import 'package:beltech/core/widgets/secondary_page_shell.dart';
import 'package:beltech/core/widgets/section_header.dart';
import 'package:beltech/features/review/presentation/providers/review_providers.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class WeekReviewScreen extends ConsumerWidget {
  const WeekReviewScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final reviewState = ref.watch(weekReviewDataProvider);

    return SecondaryPageShell(
      title: 'Week Review',
      glowColor: AppColors.glowViolet,
      child: reviewState.when(
        data: (data) => _ReviewContent(review: data),
        loading: () => _LoadingReview(),
        error: (_, __) => AppEmptyState(
          icon: Icons.error_outline,
          title: 'Unable to load week review',
          subtitle: 'Please try again',
          action: TextButton(
            onPressed: () => ref.invalidate(weekReviewDataProvider),
            child: const Text('Retry'),
          ),
        ),
      ),
    );
  }
}

class _ReviewContent extends StatelessWidget {
  const _ReviewContent({required this.review});

  final WeekReviewData review;

  @override
  Widget build(BuildContext context) {
    final netAmount = review.netKes;
    final isPositive = netAmount >= 0;
    final completionRate = review.completionRateThisWeek;
    final completionMessage = review.tasksDueThisWeek == 0
        ? 'Set due dates this week to unlock better tracking.'
        : 'You completed ${(completionRate * 100).toStringAsFixed(0)}% of tasks due this week.';

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        GlassCard(
          tone: GlassCardTone.accent,
          accentColor: AppColors.violet,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'Your momentum this week',
                style: AppTypography.sectionTitle(context),
              ),
              const SizedBox(height: 8),
              Text(
                completionMessage,
                style: AppTypography.bodySm(context),
              ),
            ],
          ),
        ),
        const SizedBox(height: AppSpacing.sectionGap),
        _StatGrid(
          topLeft: (
            label: 'Tasks Done',
            value: '${review.completedThisWeek}',
            color: AppColors.success
          ),
          topRight: (
            label: 'Open Tasks',
            value: '${review.pendingCount}',
            color: AppColors.warning
          ),
          bottomLeft: (
            label: 'Weekly Spend',
            value: CurrencyFormatter.money(review.weeklySpendKes),
            color: AppColors.danger
          ),
          bottomRight: (
            label: 'Income',
            value: CurrencyFormatter.money(review.weeklyIncomeKes),
            color: AppColors.success
          ),
        ),
        const SizedBox(height: AppSpacing.sectionGap),
        GlassCard(
          tone: GlassCardTone.muted,
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text('Net this week', style: AppTypography.cardTitle(context)),
              Text(
                CurrencyFormatter.money(netAmount),
                style: AppTypography.amount(context).copyWith(
                  color: isPositive ? AppColors.success : AppColors.danger,
                ),
                maxLines: 1,
                softWrap: false,
                overflow: TextOverflow.fade,
              ),
            ],
          ),
        ),
        const SizedBox(height: AppSpacing.sectionGap),
        SectionHeader('Trends vs Last Week'),
        _TrendGrid(review: review),
        const SizedBox(height: AppSpacing.sectionGap),
        SectionHeader('Actionable Insights'),
        const SizedBox(height: 8),
        ...review.insights.map((insight) => Padding(
              padding: const EdgeInsets.only(bottom: AppSpacing.listGap),
              child: _InsightCard(insight: insight),
            )),
        const SizedBox(height: AppSpacing.sectionGap),
        SectionHeader('Coming Up'),
        if (review.upcomingEventsCount == 0)
          AppEmptyState(
            icon: Icons.calendar_today_outlined,
            title: 'No upcoming events',
            subtitle: 'You\'re all clear for now',
          )
        else
          GlassCard(
            tone: GlassCardTone.muted,
            child: Text(
              '${review.upcomingEventsCount} upcoming event${review.upcomingEventsCount != 1 ? 's' : ''}',
              style: AppTypography.bodySm(context),
            ),
          ),
      ],
    );
  }
}

class _TrendGrid extends StatelessWidget {
  const _TrendGrid({required this.review});

  final WeekReviewData review;

  @override
  Widget build(BuildContext context) {
    final completionDelta =
        review.completionRateThisWeek - review.completionRateLastWeek;
    return Column(
      children: [
        Row(
          children: [
            Expanded(
              child: _TrendCard(
                label: 'Spend',
                value: CurrencyFormatter.money(review.weeklySpendKes),
                delta: _deltaMoney(review.spendDeltaKes),
                deltaColor: review.spendDeltaKes <= 0
                    ? AppColors.success
                    : AppColors.danger,
                icon: Icons.trending_down,
              ),
            ),
            const SizedBox(width: AppSpacing.listGap),
            Expanded(
              child: _TrendCard(
                label: 'Income',
                value: CurrencyFormatter.money(review.weeklyIncomeKes),
                delta: _deltaMoney(review.incomeDeltaKes),
                deltaColor: review.incomeDeltaKes >= 0
                    ? AppColors.success
                    : AppColors.warning,
                icon: Icons.trending_up,
              ),
            ),
          ],
        ),
        const SizedBox(height: AppSpacing.listGap),
        Row(
          children: [
            Expanded(
              child: _TrendCard(
                label: 'Net',
                value: CurrencyFormatter.money(review.netKes),
                delta: _deltaMoney(review.netDeltaKes),
                deltaColor: review.netDeltaKes >= 0
                    ? AppColors.success
                    : AppColors.danger,
                icon: Icons.account_balance_wallet_outlined,
              ),
            ),
            const SizedBox(width: AppSpacing.listGap),
            Expanded(
              child: _TrendCard(
                label: 'Task Rate',
                value:
                    '${(review.completionRateThisWeek * 100).toStringAsFixed(0)}%',
                delta: _deltaPercent(completionDelta),
                deltaColor: completionDelta >= 0
                    ? AppColors.success
                    : AppColors.warning,
                icon: Icons.task_alt_outlined,
              ),
            ),
          ],
        ),
      ],
    );
  }
}

class _TrendCard extends StatelessWidget {
  const _TrendCard({
    required this.label,
    required this.value,
    required this.delta,
    required this.deltaColor,
    required this.icon,
  });

  final String label;
  final String value;
  final String delta;
  final Color deltaColor;
  final IconData icon;

  @override
  Widget build(BuildContext context) {
    return GlassCard(
      tone: GlassCardTone.muted,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: 18, color: AppColors.textSecondary),
          const SizedBox(height: 8),
          Text(
            value,
            style: AppTypography.amount(context),
            maxLines: 1,
            softWrap: false,
            overflow: TextOverflow.fade,
          ),
          const SizedBox(height: 4),
          Text(label, style: AppTypography.bodySm(context)),
          const SizedBox(height: 6),
          Text(
            delta,
            style: AppTypography.bodySm(context).copyWith(
              color: deltaColor,
              fontWeight: FontWeight.w600,
            ),
            maxLines: 1,
            softWrap: false,
            overflow: TextOverflow.fade,
          ),
        ],
      ),
    );
  }
}

class _InsightCard extends StatelessWidget {
  const _InsightCard({required this.insight});

  final WeekReviewInsight insight;

  @override
  Widget build(BuildContext context) {
    final color = switch (insight.tone) {
      WeekReviewInsightTone.positive => AppColors.success,
      WeekReviewInsightTone.caution => AppColors.warning,
      WeekReviewInsightTone.neutral => AppColors.accent,
    };
    final icon = switch (insight.tone) {
      WeekReviewInsightTone.positive => Icons.check_circle_outline,
      WeekReviewInsightTone.caution => Icons.warning_amber_rounded,
      WeekReviewInsightTone.neutral => Icons.lightbulb_outline_rounded,
    };

    return GlassCard(
      tone: GlassCardTone.muted,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 30,
            height: 30,
            decoration: BoxDecoration(
              color: color.withValues(alpha: 0.16),
              shape: BoxShape.circle,
            ),
            child: Icon(icon, size: 16, color: color),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  insight.title,
                  style: AppTypography.cardTitle(context),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 4),
                Text(
                  insight.detail,
                  style: AppTypography.bodySm(context),
                  maxLines: 3,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _StatGrid extends StatelessWidget {
  const _StatGrid({
    required this.topLeft,
    required this.topRight,
    required this.bottomLeft,
    required this.bottomRight,
  });

  final ({String label, String value, Color color}) topLeft;
  final ({String label, String value, Color color}) topRight;
  final ({String label, String value, Color color}) bottomLeft;
  final ({String label, String value, Color color}) bottomRight;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Row(
          children: [
            Expanded(
              child: _StatCard(
                label: topLeft.label,
                value: topLeft.value,
                color: topLeft.color,
              ),
            ),
            const SizedBox(width: AppSpacing.listGap),
            Expanded(
              child: _StatCard(
                label: topRight.label,
                value: topRight.value,
                color: topRight.color,
              ),
            ),
          ],
        ),
        const SizedBox(height: AppSpacing.listGap),
        Row(
          children: [
            Expanded(
              child: _StatCard(
                label: bottomLeft.label,
                value: bottomLeft.value,
                color: bottomLeft.color,
              ),
            ),
            const SizedBox(width: AppSpacing.listGap),
            Expanded(
              child: _StatCard(
                label: bottomRight.label,
                value: bottomRight.value,
                color: bottomRight.color,
              ),
            ),
          ],
        ),
      ],
    );
  }
}

class _StatCard extends StatelessWidget {
  const _StatCard({
    required this.label,
    required this.value,
    required this.color,
  });

  final String label;
  final String value;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return GlassCard(
      tone: GlassCardTone.muted,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 32,
            height: 32,
            decoration: BoxDecoration(
              color: color.withValues(alpha: 0.16),
              shape: BoxShape.circle,
            ),
            child: Icon(
              _iconFor(label),
              size: 16,
              color: color,
            ),
          ),
          const SizedBox(height: 12),
          Text(
            value,
            style: AppTypography.amountLg(context),
            maxLines: 1,
            softWrap: false,
            overflow: TextOverflow.fade,
          ),
          const SizedBox(height: 4),
          Text(
            label,
            style: AppTypography.bodySm(context),
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
          ),
        ],
      ),
    );
  }

  IconData _iconFor(String label) {
    return switch (label) {
      'Tasks Done' => Icons.check_circle_outline,
      'Open Tasks' => Icons.pending_actions,
      'Weekly Spend' => Icons.trending_down,
      'Income' => Icons.trending_up,
      _ => Icons.info_outline,
    };
  }
}

class _LoadingReview extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        GlassCard(
          tone: GlassCardTone.muted,
          child: Container(
            height: 80,
            alignment: Alignment.center,
            child: const LoadingIndicator(),
          ),
        ),
        const SizedBox(height: AppSpacing.sectionGap),
        GlassCard(
          tone: GlassCardTone.muted,
          child: Container(
            height: 120,
            alignment: Alignment.center,
            child: const LoadingIndicator(),
          ),
        ),
        const SizedBox(height: AppSpacing.sectionGap),
        GlassCard(
          tone: GlassCardTone.muted,
          child: Container(
            height: 60,
            alignment: Alignment.center,
            child: const LoadingIndicator(),
          ),
        ),
      ],
    );
  }
}

String _deltaMoney(double delta) {
  final sign = delta >= 0 ? '+' : '-';
  return '$sign${CurrencyFormatter.money(delta.abs())} vs last week';
}

String _deltaPercent(double delta) {
  final sign = delta >= 0 ? '+' : '-';
  return '$sign${(delta.abs() * 100).toStringAsFixed(0)}% vs last week';
}
