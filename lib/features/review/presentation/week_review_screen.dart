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

part 'week_review_screen_stats.dart';
part 'week_review_screen_trends.dart';

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
