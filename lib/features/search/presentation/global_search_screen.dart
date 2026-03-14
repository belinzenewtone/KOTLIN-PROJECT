import 'package:beltech/core/navigation/shell_providers.dart';
import 'package:beltech/core/theme/app_colors.dart';
import 'package:beltech/core/widgets/app_capsule.dart';
import 'package:beltech/core/widgets/error_message.dart';
import 'package:beltech/core/widgets/glass_card.dart';
import 'package:beltech/core/widgets/loading_indicator.dart';
import 'package:beltech/core/widgets/secondary_page_shell.dart';
import 'package:beltech/features/search/domain/entities/global_search_result.dart';
import 'package:beltech/features/search/presentation/providers/global_search_providers.dart';
import 'package:go_router/go_router.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class GlobalSearchScreen extends ConsumerWidget {
  const GlobalSearchScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final query = ref.watch(globalSearchQueryProvider);
    final activeFilter = ref.watch(globalSearchKindFilterProvider);
    final resultsState = ref.watch(filteredSearchResultsProvider);
    final textTheme = Theme.of(context).textTheme;

    return SecondaryPageShell(
      title: 'Global Search',
      glowColor: AppColors.glowBlue,
      scrollable: false,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // ── Search bar ──────────────────────────────────────────────────────
          TextField(
            onChanged: (value) {
              ref.read(globalSearchQueryProvider.notifier).state = value;
            },
            decoration: const InputDecoration(
              hintText: 'Search expenses, tasks, events, budgets...',
              prefixIcon: Icon(Icons.search),
            ),
          ),
          const SizedBox(height: 10),

          // ── Kind filter chips ────────────────────────────────────────────────
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: [
                _FilterChip(
                  label: 'All',
                  isActive: activeFilter.isEmpty,
                  color: AppColors.accent,
                  onTap: () => ref
                      .read(globalSearchKindFilterProvider.notifier)
                      .state = const {},
                ),
                const SizedBox(width: 6),
                for (final kind in GlobalSearchKind.values) ...[
                  _FilterChip(
                    label: _kindLabel(kind),
                    isActive: activeFilter.contains(kind),
                    color: _kindColor(kind),
                    onTap: () {
                      final current = Set<GlobalSearchKind>.from(
                          ref.read(globalSearchKindFilterProvider));
                      if (current.contains(kind)) {
                        current.remove(kind);
                      } else {
                        current.add(kind);
                      }
                      ref
                          .read(globalSearchKindFilterProvider.notifier)
                          .state = current;
                    },
                  ),
                  const SizedBox(width: 6),
                ],
              ],
            ),
          ),
          const SizedBox(height: 12),

          // ── Results ──────────────────────────────────────────────────────────
          Expanded(
            child: resultsState.when(
              data: (results) {
                if (query.trim().isEmpty) {
                  return Align(
                    alignment: Alignment.topCenter,
                    child: ConstrainedBox(
                      constraints: const BoxConstraints(maxWidth: 520),
                      child: const GlassCard(
                        child: Text('Start typing to search across the app'),
                      ),
                    ),
                  );
                }
                if (results.isEmpty) {
                  return Align(
                    alignment: Alignment.topCenter,
                    child: ConstrainedBox(
                      constraints: const BoxConstraints(maxWidth: 520),
                      child: const GlassCard(
                        child: Text('No matching results'),
                      ),
                    ),
                  );
                }
                return ListView.separated(
                  itemCount: results.length,
                  separatorBuilder: (_, __) => const SizedBox(height: 8),
                  itemBuilder: (context, index) {
                    final result = results[index];
                    final kindColor = _kindColor(result.kind);
                    return GlassCard(
                      child: InkWell(
                        borderRadius: BorderRadius.circular(16),
                        onTap: () => _navigateTo(context, ref, result),
                        child: Row(
                          children: [
                            CircleAvatar(
                              backgroundColor:
                                  kindColor.withValues(alpha: 0.18),
                              child: Icon(_iconFor(result.kind),
                                  color: kindColor, size: 18),
                            ),
                            const SizedBox(width: 10),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(result.primaryText,
                                      style: textTheme.bodyLarge,
                                      maxLines: 1,
                                      overflow: TextOverflow.ellipsis),
                                  Text(
                                    result.secondaryText,
                                    style: textTheme.bodyMedium,
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                  ),
                                ],
                              ),
                            ),
                            Column(
                              crossAxisAlignment: CrossAxisAlignment.end,
                              children: [
                                if (result.trailingText.isNotEmpty)
                                  Text(
                                    result.trailingText,
                                    maxLines: 1,
                                    softWrap: false,
                                    overflow: TextOverflow.ellipsis,
                                    style: textTheme.bodyMedium?.copyWith(
                                        fontWeight: FontWeight.w600),
                                  ),
                                AppCapsule(
                                  label: _kindLabel(result.kind),
                                  color: kindColor,
                                  variant: AppCapsuleVariant.subtle,
                                  size: AppCapsuleSize.sm,
                                ),
                              ],
                            ),
                            const SizedBox(width: 4),
                            const Icon(Icons.chevron_right, size: 18),
                          ],
                        ),
                      ),
                    );
                  },
                );
              },
              loading: () => const Center(child: LoadingIndicator()),
              error: (_, __) => ErrorMessage(
                label: 'Search failed',
                onRetry: () => ref.invalidate(globalSearchResultsProvider),
              ),
            ),
          ),
        ],
      ),
    );
  }

  String _kindLabel(GlobalSearchKind kind) {
    return switch (kind) {
      GlobalSearchKind.expense => 'Expense',
      GlobalSearchKind.income => 'Income',
      GlobalSearchKind.task => 'Task',
      GlobalSearchKind.event => 'Event',
      GlobalSearchKind.budget => 'Budget',
      GlobalSearchKind.recurring => 'Recurring',
    };
  }

  Color _kindColor(GlobalSearchKind kind) {
    return switch (kind) {
      GlobalSearchKind.expense => AppColors.danger,
      GlobalSearchKind.income => AppColors.success,
      GlobalSearchKind.task => AppColors.teal,
      GlobalSearchKind.event => AppColors.violet,
      GlobalSearchKind.budget => AppColors.warning,
      GlobalSearchKind.recurring => AppColors.accent,
    };
  }

  IconData _iconFor(GlobalSearchKind kind) {
    return switch (kind) {
      GlobalSearchKind.expense => Icons.receipt_long_outlined,
      GlobalSearchKind.income => Icons.account_balance_wallet_outlined,
      GlobalSearchKind.task => Icons.check_circle_outline,
      GlobalSearchKind.event => Icons.calendar_month_outlined,
      GlobalSearchKind.budget => Icons.savings_outlined,
      GlobalSearchKind.recurring => Icons.autorenew,
    };
  }

  void _navigateTo(
      BuildContext context, WidgetRef ref, GlobalSearchResult result) {
    ref.read(globalSearchDeepLinkTargetProvider.notifier).state =
        GlobalSearchDeepLinkTarget(
      kind: result.kind,
      recordId: result.recordId,
      recordDate: result.recordDate,
    );

    switch (result.kind) {
      case GlobalSearchKind.expense:
        ref.read(shellTabIndexProvider.notifier).state = 2;
        context.pop();
        return;
      case GlobalSearchKind.income:
        context.pushNamed('income');
        return;
      case GlobalSearchKind.task:
        ref.read(shellTabIndexProvider.notifier).state = 3;
        context.pop();
        return;
      case GlobalSearchKind.event:
        ref.read(shellTabIndexProvider.notifier).state = 1;
        context.pop();
        return;
      case GlobalSearchKind.budget:
        context.pushNamed('budget');
        return;
      case GlobalSearchKind.recurring:
        context.pushNamed('recurring');
        return;
    }
  }
}

// ── Filter chip ───────────────────────────────────────────────────────────────

class _FilterChip extends StatelessWidget {
  const _FilterChip({
    required this.label,
    required this.isActive,
    required this.color,
    required this.onTap,
  });

  final String label;
  final bool isActive;
  final Color color;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return AppCapsule(
      label: label,
      color: isActive ? color : AppColors.textMuted,
      variant: isActive ? AppCapsuleVariant.solid : AppCapsuleVariant.subtle,
      size: AppCapsuleSize.md, // slightly larger for interactive chips
      onTap: onTap,
    );
  }
}
