import 'package:beltech/core/theme/app_spacing.dart';
import 'package:beltech/core/utils/currency_formatter.dart';
import 'package:beltech/core/widgets/app_feedback.dart';
import 'package:beltech/core/widgets/error_message.dart';
import 'package:beltech/core/widgets/glass_card.dart';
import 'package:beltech/core/widgets/loading_indicator.dart';
import 'package:beltech/features/budget/domain/entities/budget_snapshot.dart';
import 'package:beltech/features/budget/domain/entities/budget_target.dart';
import 'package:beltech/features/budget/presentation/providers/budget_providers.dart';
import 'package:beltech/features/budget/presentation/widgets/budget_dialogs.dart';
import 'package:beltech/features/search/domain/entities/global_search_result.dart';
import 'package:beltech/features/search/presentation/providers/global_search_providers.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class BudgetScreen extends ConsumerWidget {
  const BudgetScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final month = ref.watch(budgetMonthProvider);
    final snapshotState = ref.watch(budgetSnapshotProvider);
    final targetsState = ref.watch(budgetTargetsProvider);
    final writeState = ref.watch(budgetWriteControllerProvider);
    final textTheme = Theme.of(context).textTheme;

    ref.listen<AsyncValue<void>>(budgetWriteControllerProvider,
        (previous, next) {
      if (next.hasError) {
        AppFeedback.error(context, 'Unable to save budget changes.');
      }
    });

    return Scaffold(
      appBar: AppBar(
        title: const Text('Budgets'),
        actions: [
          IconButton(
            onPressed: () {
              final previous = DateTime(month.year, month.month - 1, 1);
              ref.read(budgetMonthProvider.notifier).state = previous;
            },
            icon: const Icon(Icons.chevron_left),
          ),
          Center(
            child: Text('${_monthName(month.month)} ${month.year}'),
          ),
          IconButton(
            onPressed: () {
              final nextMonth = DateTime(month.year, month.month + 1, 1);
              ref.read(budgetMonthProvider.notifier).state = nextMonth;
            },
            icon: const Icon(Icons.chevron_right),
          ),
        ],
      ),
      body: SafeArea(
        child: ListView(
          padding: AppSpacing.sectionPadding(context),
          children: [
            snapshotState.when(
              data: (snapshot) => _BudgetSummaryCard(snapshot: snapshot),
              loading: () => Center(child: LoadingIndicator()),
              error: (_, __) => ErrorMessage(
                label: 'Unable to load budget snapshot',
                onRetry: () => ref.invalidate(budgetSnapshotProvider),
              ),
            ),
            const SizedBox(height: 12),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text('Category Targets', style: textTheme.titleMedium),
                FilledButton.icon(
                  onPressed: writeState.isLoading
                      ? null
                      : () async {
                          final input = await showBudgetTargetDialog(context);
                          if (input == null) {
                            return;
                          }
                          await ref
                              .read(budgetWriteControllerProvider.notifier)
                              .saveTarget(
                                category: input.category,
                                monthlyLimitKes: input.monthlyLimitKes,
                              );
                        },
                  icon: const Icon(Icons.add),
                  label: const Text('Add'),
                ),
              ],
            ),
            const SizedBox(height: 8),
            targetsState.when(
              data: (targets) {
                _consumeSearchTarget(context, ref, targets);
                if (targets.isEmpty) {
                  return const GlassCard(
                    child: Text('No category budgets yet'),
                  );
                }
                return Column(
                  children: targets
                      .map(
                        (target) => Padding(
                          padding: const EdgeInsets.only(bottom: 10),
                          child: GlassCard(
                            child: Row(
                              children: [
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    children: [
                                      Text(target.category,
                                          style: textTheme.bodyLarge),
                                      Text(
                                        CurrencyFormatter.money(
                                            target.monthlyLimitKes),
                                        style: textTheme.bodyMedium,
                                        maxLines: 1,
                                        softWrap: false,
                                        overflow: TextOverflow.fade,
                                      ),
                                    ],
                                  ),
                                ),
                                IconButton(
                                  onPressed: writeState.isLoading
                                      ? null
                                      : () async {
                                          final input =
                                              await showBudgetTargetDialog(
                                            context,
                                            initialCategory: target.category,
                                            initialLimit:
                                                target.monthlyLimitKes,
                                          );
                                          if (input == null) {
                                            return;
                                          }
                                          await ref
                                              .read(
                                                  budgetWriteControllerProvider
                                                      .notifier)
                                              .saveTarget(
                                                category: input.category,
                                                monthlyLimitKes:
                                                    input.monthlyLimitKes,
                                              );
                                        },
                                  icon: const Icon(Icons.edit_outlined),
                                ),
                                IconButton(
                                  onPressed: writeState.isLoading
                                      ? null
                                      : () async {
                                          await ref
                                              .read(
                                                  budgetWriteControllerProvider
                                                      .notifier)
                                              .deleteTarget(target.id);
                                        },
                                  icon: const Icon(Icons.delete_outline),
                                ),
                              ],
                            ),
                          ),
                        ),
                      )
                      .toList(),
                );
              },
              loading: () => Center(child: LoadingIndicator()),
              error: (_, __) => ErrorMessage(
                label: 'Unable to load budget targets',
                onRetry: () => ref.invalidate(budgetTargetsProvider),
              ),
            ),
          ],
        ),
      ),
    );
  }

  void _consumeSearchTarget(
    BuildContext context,
    WidgetRef ref,
    List<BudgetTarget> targets,
  ) {
    final target = ref.read(globalSearchDeepLinkTargetProvider);
    if (target?.kind != GlobalSearchKind.budget) {
      return;
    }

    ref.read(globalSearchDeepLinkTargetProvider.notifier).state = null;

    final recordId = target?.recordId;
    if (recordId == null) {
      return;
    }

    final item = targets.where((budget) => budget.id == recordId).firstOrNull;
    if (item == null) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (context.mounted) {
          AppFeedback.info(context, 'This budget target no longer exists.');
        }
      });
      return;
    }

    WidgetsBinding.instance.addPostFrameCallback((_) async {
      if (!context.mounted) {
        return;
      }
      final input = await showBudgetTargetDialog(
        context,
        initialCategory: item.category,
        initialLimit: item.monthlyLimitKes,
      );
      if (input == null) {
        return;
      }
      await ref.read(budgetWriteControllerProvider.notifier).saveTarget(
            category: input.category,
            monthlyLimitKes: input.monthlyLimitKes,
          );
    });
  }

  String _monthName(int month) {
    const names = [
      'Jan',
      'Feb',
      'Mar',
      'Apr',
      'May',
      'Jun',
      'Jul',
      'Aug',
      'Sep',
      'Oct',
      'Nov',
      'Dec'
    ];
    return names[month - 1];
  }
}

class _BudgetSummaryCard extends StatefulWidget {
  const _BudgetSummaryCard({required this.snapshot});

  final BudgetSnapshot snapshot;

  @override
  State<_BudgetSummaryCard> createState() => _BudgetSummaryCardState();
}

class _BudgetSummaryCardState extends State<_BudgetSummaryCard> {
  bool _showAll = false;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final items = widget.snapshot.items;
    final visibleItems = _showAll ? items : items.take(6).toList();
    final hasMore = items.length > 6;
    return GlassCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Monthly Budget Usage', style: textTheme.titleMedium),
          const SizedBox(height: 8),
          Text(
            '${CurrencyFormatter.money(widget.snapshot.totalSpentKes)} / ${CurrencyFormatter.money(widget.snapshot.totalLimitKes)}',
            style: textTheme.bodyLarge,
            maxLines: 1,
            softWrap: false,
            overflow: TextOverflow.fade,
          ),
          const SizedBox(height: 8),
          for (final item in visibleItems) ...[
            _BudgetProgressRow(item: item),
            const SizedBox(height: 8),
          ],
          if (hasMore)
            TextButton(
              onPressed: () => setState(() => _showAll = !_showAll),
              child: Text(
                _showAll
                    ? 'Show fewer'
                    : 'Show all categories (${items.length})',
              ),
            ),
        ],
      ),
    );
  }
}

class _BudgetProgressRow extends StatelessWidget {
  const _BudgetProgressRow({required this.item});

  final BudgetCategoryItem item;

  @override
  Widget build(BuildContext context) {
    final exceeded = item.spentKes > item.monthlyLimitKes;
    return Row(
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(item.category),
              const SizedBox(height: 4),
              LinearProgressIndicator(
                value: item.usageRatio,
                minHeight: 6,
                borderRadius: BorderRadius.circular(100),
                backgroundColor: Colors.white12,
                valueColor: AlwaysStoppedAnimation<Color>(
                  exceeded ? Colors.redAccent : Colors.blueAccent,
                ),
              ),
            ],
          ),
        ),
        const SizedBox(width: 10),
        Text(
          CurrencyFormatter.money(item.spentKes),
          maxLines: 1,
          softWrap: false,
          overflow: TextOverflow.fade,
        ),
      ],
    );
  }
}
