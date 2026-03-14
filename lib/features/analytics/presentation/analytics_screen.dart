import 'package:beltech/core/theme/app_colors.dart';
import 'package:beltech/core/theme/app_spacing.dart';
import 'package:beltech/core/theme/app_typography.dart';
import 'package:beltech/core/widgets/glass_card.dart';
import 'package:beltech/core/widgets/secondary_page_shell.dart';
import 'package:beltech/core/widgets/section_header.dart';
import 'package:beltech/features/analytics/domain/entities/analytics_snapshot.dart';
import 'package:beltech/features/analytics/presentation/providers/analytics_providers.dart';
import 'package:beltech/features/analytics/presentation/widgets/analytics_bar_chart.dart';
import 'package:beltech/features/analytics/presentation/widgets/analytics_category_breakdown.dart';
import 'package:beltech/features/analytics/presentation/widgets/analytics_overview_cards.dart';
import 'package:beltech/features/analytics/presentation/widgets/analytics_trend_chart.dart';
import 'package:beltech/core/widgets/loading_indicator.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class AnalyticsScreen extends ConsumerWidget {
  const AnalyticsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final snapshotState = ref.watch(analyticsSnapshotProvider);
    final period = ref.watch(analyticsPeriodProvider);

    return SecondaryPageShell(
      title: 'Analytics',
      glowColor: AppColors.glowBlue,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _PeriodSelector(period: period),
          SizedBox(height: AppSpacing.sectionGap),
          snapshotState.when(
            data: (snapshot) => _AnalyticsContent(
              snapshot: snapshot,
              period: period,
            ),
            loading: () => _LoadingAnalytics(),
            error: (_, __) => GlassCard(
              tone: GlassCardTone.muted,
              child: Column(
                children: [
                  Icon(Icons.error_outline, color: AppColors.danger),
                  const SizedBox(height: 8),
                  Text(
                    'Unable to load analytics',
                    style: AppTypography.bodySm(context),
                  ),
                  const SizedBox(height: 12),
                  TextButton(
                    onPressed: () => ref.invalidate(analyticsSnapshotProvider),
                    child: const Text('Retry'),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _PeriodSelector extends ConsumerWidget {
  const _PeriodSelector({required this.period});

  final AnalyticsPeriod period;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Row(
      children: [
        Expanded(
          child: GlassCard(
            tone: period == AnalyticsPeriod.week
                ? GlassCardTone.accent
                : GlassCardTone.muted,
            onTap: () {
              ref.read(analyticsPeriodProvider.notifier).state =
                  AnalyticsPeriod.week;
            },
            child: Center(
              child: Padding(
                padding: const EdgeInsets.symmetric(vertical: 12),
                child: Text(
                  'Weekly',
                  style: AppTypography.cardTitle(context).copyWith(
                    color: period == AnalyticsPeriod.week
                        ? AppColors.accent
                        : AppColors.textMuted,
                  ),
                ),
              ),
            ),
          ),
        ),
        const SizedBox(width: AppSpacing.listGap),
        Expanded(
          child: GlassCard(
            tone: period == AnalyticsPeriod.month
                ? GlassCardTone.accent
                : GlassCardTone.muted,
            onTap: () {
              ref.read(analyticsPeriodProvider.notifier).state =
                  AnalyticsPeriod.month;
            },
            child: Center(
              child: Padding(
                padding: const EdgeInsets.symmetric(vertical: 12),
                child: Text(
                  'Monthly',
                  style: AppTypography.cardTitle(context).copyWith(
                    color: period == AnalyticsPeriod.month
                        ? AppColors.accent
                        : AppColors.textMuted,
                  ),
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }
}

class _LoadingAnalytics extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        GlassCard(
          tone: GlassCardTone.muted,
          child: Container(
            height: 180,
            alignment: Alignment.center,
            child: const LoadingIndicator(),
          ),
        ),
        SizedBox(height: AppSpacing.cardGap),
        GlassCard(
          tone: GlassCardTone.muted,
          child: Container(
            height: 180,
            alignment: Alignment.center,
            child: const LoadingIndicator(),
          ),
        ),
        SizedBox(height: AppSpacing.cardGap),
        GlassCard(
          tone: GlassCardTone.muted,
          child: Container(
            height: 180,
            alignment: Alignment.center,
            child: const LoadingIndicator(),
          ),
        ),
        SizedBox(height: AppSpacing.cardGap),
        GlassCard(
          tone: GlassCardTone.muted,
          child: Container(
            height: 180,
            alignment: Alignment.center,
            child: const LoadingIndicator(),
          ),
        ),
      ],
    );
  }
}

class _AnalyticsContent extends StatelessWidget {
  const _AnalyticsContent({
    required this.snapshot,
    required this.period,
  });

  final AnalyticsSnapshot snapshot;
  final AnalyticsPeriod period;

  @override
  Widget build(BuildContext context) {
    final trendPoints = switch (period) {
      AnalyticsPeriod.week => snapshot.weeklySpending,
      AnalyticsPeriod.month => snapshot.monthlySpending,
    };
    final trendTitle = period == AnalyticsPeriod.week
        ? 'Weekly Spending Trend'
        : 'Monthly Spending Trend';

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SectionHeader('Overview', topPadding: 0),
        AnalyticsOverviewCards(snapshot: snapshot),
        SizedBox(height: AppSpacing.sectionGap),
        SectionHeader('Spending Trends'),
        AnalyticsTrendChart(
          title: trendTitle,
          points: trendPoints,
        ),
        SizedBox(height: AppSpacing.sectionGap),
        SectionHeader('Distribution'),
        AnalyticsBarChart(
          title: period == AnalyticsPeriod.week
              ? 'Weekly Spend Distribution'
              : 'Daily Spend Distribution',
          points: trendPoints,
        ),
        SizedBox(height: AppSpacing.sectionGap),
        SectionHeader('Categories'),
        AnalyticsCategoryBreakdown(
          categories: snapshot.categoryBreakdown,
        ),
      ],
    );
  }
}
