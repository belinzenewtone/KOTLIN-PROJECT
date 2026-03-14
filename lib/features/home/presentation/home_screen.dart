import 'package:beltech/core/di/sync_providers.dart';
import 'package:beltech/core/theme/app_colors.dart';
import 'package:beltech/core/theme/app_spacing.dart';
import 'package:beltech/core/widgets/app_button.dart';
import 'package:beltech/core/widgets/app_empty_state.dart';
import 'package:beltech/core/widgets/app_skeleton.dart';
import 'package:beltech/core/widgets/glass_card.dart';
import 'package:beltech/core/widgets/page_header.dart';
import 'package:beltech/core/widgets/page_shell.dart';
import 'package:beltech/core/widgets/section_header.dart';
import 'package:beltech/core/widgets/stagger_reveal.dart';
import 'package:beltech/core/navigation/shell_providers.dart';
import 'package:beltech/features/home/domain/entities/home_overview.dart';
import 'package:beltech/features/home/presentation/providers/home_providers.dart';
import 'package:beltech/features/home/presentation/widgets/home_spending_cards.dart';
import 'package:beltech/features/home/presentation/widgets/home_quick_actions.dart';
import 'package:beltech/features/home/presentation/widgets/home_sync_banner.dart';
import 'package:beltech/features/home/presentation/widgets/spending_chart.dart';
import 'package:beltech/features/profile/presentation/providers/profile_providers.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

class HomeScreen extends ConsumerStatefulWidget {
  const HomeScreen({super.key});

  @override
  ConsumerState<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends ConsumerState<HomeScreen> {
  @override
  void initState() {
    super.initState();
    // Trigger a foreground sync on screen mount so the banner reflects
    // real-time status as soon as the user lands on home.
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        ref.read(syncStatusProvider.notifier).runSync();
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final overviewState = ref.watch(homeOverviewProvider);
    final profileState = ref.watch(profileProvider);
    final firstName =
        profileState.valueOrNull?.name.trim().split(' ').first ?? '';
    final greeting = _greeting(firstName);
    final todayLabel = _todayLabel(DateTime.now());
    final initials = firstName.isNotEmpty
        ? firstName[0].toUpperCase()
        : (profileState.valueOrNull?.email.isNotEmpty == true
            ? profileState.valueOrNull!.email[0].toUpperCase()
            : 'B');

    return PageShell(
      scrollable: true,
      glowColor: AppColors.glowBlue,
      secondaryGlowColor: AppColors.glowTeal,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // ── Header ──────────────────────────────────────────────────────────
          PageHeader(
            eyebrow: todayLabel,
            title: greeting,
            subtitle: "Here's your day at a glance",
            action: GestureDetector(
              onTap: () {
                HapticFeedback.lightImpact();
                ref.read(shellTabIndexProvider.notifier).state = 5;
              },
              child: CircleAvatar(
                radius: 20,
                backgroundColor: AppColors.accent.withValues(alpha: 0.22),
                child: Text(
                  initials,
                  style: TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.w700,
                    color: AppColors.accent,
                  ),
                ),
              ),
            ),
          ),

          // ── Sync status banner ───────────────────────────────────────────────
          const HomeSyncBanner(),

          // ── Content ──────────────────────────────────────────────────────────
          overviewState.when(
            loading: () => const HomeSkeletonList(),
            error: (_, __) => AppEmptyState(
              icon: Icons.cloud_off_rounded,
              title: 'Could not load dashboard',
              subtitle: 'Check your connection and try again',
              iconColor: AppColors.danger,
              action: AppButton(
                label: 'Retry',
                onPressed: () => ref.invalidate(homeOverviewProvider),
                variant: AppButtonVariant.secondary,
                size: AppButtonSize.sm,
              ),
            ),
            data: (overview) => _HomeOverviewSection(overview: overview),
          ),
        ],
      ),
    );
  }

  String _greeting(String firstName) {
    final hour = DateTime.now().hour;
    final salutation = switch (hour) {
      >= 5 && < 12 => 'Good Morning',
      >= 12 && < 17 => 'Good Afternoon',
      >= 17 && < 21 => 'Good Evening',
      _ => 'Good Night',
    };
    return firstName.isEmpty ? salutation : '$salutation, $firstName';
  }

  String _todayLabel(DateTime now) {
    final weekday = DateFormat('EEEE').format(now);
    final monthDay = DateFormat('MMMM d').format(now);
    return '$weekday, $monthDay${_ordinalSuffix(now.day)}';
  }

  String _ordinalSuffix(int day) {
    if (day >= 11 && day <= 13) {
      return 'th';
    }
    return switch (day % 10) {
      1 => 'st',
      2 => 'nd',
      3 => 'rd',
      _ => 'th',
    };
  }
}

// ── Dashboard overview section ────────────────────────────────────────────────

class _HomeOverviewSection extends ConsumerWidget {
  const _HomeOverviewSection({required this.overview});
  final HomeOverview overview;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        StaggerReveal(
          delay: const Duration(milliseconds: 30),
          child: HomeSpendSnapshotStrip(overview: overview),
        ),
        const SizedBox(height: AppSpacing.cardGap),
        StaggerReveal(
          delay: const Duration(milliseconds: 80),
          child: const HomeAiInsightCard(),
        ),
        const SizedBox(height: AppSpacing.sectionGap),
        SectionHeader('Productivity'),
        StaggerReveal(
          delay: const Duration(milliseconds: 110),
          child: HomeProductivityCard(overview: overview),
        ),
        const SizedBox(height: AppSpacing.sectionGap),
        SectionHeader(
          'This Week',
          action: TextButton(
            onPressed: () => context.pushNamed('week-review'),
            child: Text(
              'Details',
              style: TextStyle(
                fontSize: 13,
                color: AppColors.accent,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ),
        StaggerReveal(
          delay: const Duration(milliseconds: 140),
          child: HomeBalanceCard(overview: overview),
        ),
        const SizedBox(height: AppSpacing.sectionGap),
        SectionHeader('Spending Trend'),
        StaggerReveal(
          delay: const Duration(milliseconds: 170),
          child: GlassCard(
            child: SpendingChart(dayValues: overview.weeklySpendingKes),
          ),
        ),
        const SizedBox(height: AppSpacing.sectionGap),
        SectionHeader(
          'Recent',
          action: TextButton(
            onPressed: () {
              ref.read(shellTabIndexProvider.notifier).state = 2;
            },
            child: Text(
              'See all',
              style: TextStyle(
                fontSize: 13,
                color: AppColors.accent,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ),
        if (overview.recentTransactions.isEmpty)
          AppEmptyState(
            icon: Icons.receipt_long_outlined,
            title: 'No recent transactions',
            subtitle: 'Your latest expenses will appear here',
          )
        else
          Column(
            children: [
              for (int i = 0; i < overview.recentTransactions.length; i++) ...[
                StaggerReveal(
                  delay: Duration(milliseconds: 200 + i * 40),
                  child: HomeDashboardTransactionTile(
                    tx: overview.recentTransactions[i],
                  ),
                ),
                if (i < overview.recentTransactions.length - 1)
                  const SizedBox(height: AppSpacing.listGap),
              ],
            ],
          ),
        const SizedBox(height: AppSpacing.sectionGap),
        SectionHeader('Quick Actions'),
        StaggerReveal(
          delay: const Duration(milliseconds: 280),
          child: const HomeQuickActionsGrid(),
        ),
        const SizedBox(height: 8),
      ],
    );
  }
}
