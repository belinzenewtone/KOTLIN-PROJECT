import 'package:beltech/core/theme/app_colors.dart';
import 'package:beltech/core/theme/app_motion.dart';
import 'package:beltech/core/widgets/app_feedback.dart';
import 'package:beltech/core/widgets/app_skeleton.dart';
import 'package:beltech/core/widgets/error_message.dart';
import 'package:beltech/core/widgets/page_header.dart';
import 'package:beltech/core/widgets/page_shell.dart';
import 'package:beltech/features/expenses/domain/entities/expense_item.dart';
import 'package:beltech/features/expenses/presentation/providers/expenses_providers.dart';
import 'package:beltech/features/expenses/presentation/widgets/expense_dialogs.dart';
import 'package:beltech/features/expenses/presentation/widgets/expenses_snapshot_content.dart';
import 'package:beltech/features/expenses/presentation/widgets/sms_import_dialogs.dart';
import 'package:beltech/features/search/domain/entities/global_search_result.dart';
import 'package:beltech/features/search/presentation/providers/global_search_providers.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class ExpensesScreen extends ConsumerWidget {
  const ExpensesScreen({super.key});
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final snapshotState = ref.watch(expensesSnapshotProvider);
    final selectedFilter = ref.watch(expenseFilterProvider);
    final writeState = ref.watch(expenseWriteControllerProvider);
    final contentSwitchDuration =
        AppMotion.duration(context, normalMs: 180, reducedMs: 0);
    final snapshotChild = snapshotState.when(
      data: (snapshot) {
        _consumeSearchTarget(context, ref, snapshot);
        return KeyedSubtree(
          key: const ValueKey<String>('expenses-data'),
          child: ExpensesSnapshotContent(
            snapshot: snapshot,
            selectedFilter: selectedFilter,
            busy: writeState.isLoading,
            onFilterChanged: (filter) {
              ref.read(expenseFilterProvider.notifier).state = filter;
            },
            onEditExpense: (expense) async {
              await _editExpense(context, ref, expense);
            },
            onDeleteExpense: (expense) async {
              await ref
                  .read(expenseWriteControllerProvider.notifier)
                  .deleteExpense(expense.id);
              if (context.mounted &&
                  !ref.read(expenseWriteControllerProvider).hasError) {
                AppFeedback.success(context, 'Transaction deleted', ref: ref);
              }
            },
          ),
        );
      },
      loading: () => const KeyedSubtree(
        key: ValueKey<String>('expenses-loading'),
        child: FinanceSkeletonList(),
      ),
      error: (_, __) => KeyedSubtree(
        key: const ValueKey<String>('expenses-error'),
        child: ErrorMessage(
          label: 'Unable to load expenses',
          onRetry: () => ref.invalidate(expensesSnapshotProvider),
        ),
      ),
    );
    ref.listen<AsyncValue<void>>(expenseWriteControllerProvider,
        (previous, next) {
      if (next.hasError) {
        AppFeedback.error(
            context, 'Unable to save transaction. Please try again.',
            ref: ref);
      }
    });
    return PageShell(
      scrollable: false,
      glowColor: AppColors.glowTeal,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          PageHeader(
            eyebrow: 'MONEY',
            title: 'Finance',
            subtitle: 'Your financial picture',
            action: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                IconButton(
                  tooltip: 'Import SMS',
                  onPressed: writeState.isLoading
                      ? null
                      : () async {
                          final method = await showSmsImportMethodDialog(
                            context,
                          );
                          if (method == null) {
                            return;
                          }
                          if (method == SmsImportMethod.deviceInbox) {
                            if (!context.mounted) {
                              return;
                            }
                            final window = await showSmsWindowDialog(
                              context,
                            );
                            if (window == null) {
                              return;
                            }
                            final count = await ref
                                .read(expenseWriteControllerProvider.notifier)
                                .importFromDevice(window: window);
                            if (context.mounted) {
                              final label = count == 0
                                  ? 'No MPESA messages found in ${importWindowLabel(window)}'
                                  : 'Imported $count MPESA transactions from device';
                              AppFeedback.info(context, label, ref: ref);
                            }
                            return;
                          }
                          if (!context.mounted) {
                            return;
                          }
                          final input = await showSmsImportDialog(context);
                          if (input == null || input.payload.trim().isEmpty) {
                            return;
                          }
                          final count = await ref
                              .read(expenseWriteControllerProvider.notifier)
                              .importSmsPayload(
                                input.payload,
                                window: input.window,
                              );
                          if (context.mounted) {
                            final label = count == 0
                                ? 'No MPESA messages found in ${importWindowLabel(input.window)}'
                                : 'Imported $count MPESA transactions';
                            AppFeedback.info(context, label, ref: ref);
                          }
                        },
                  icon: const Icon(Icons.file_download_outlined),
                ),
                const SizedBox(width: 4),
                Container(
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: AppColors.accent.withValues(alpha: 0.12),
                  ),
                  child: IconButton(
                    tooltip: 'Add expense',
                    onPressed: writeState.isLoading
                        ? null
                        : () async {
                            final input = await showAddExpenseDialog(context);
                            if (input == null) {
                              return;
                            }
                            await ref
                                .read(expenseWriteControllerProvider.notifier)
                                .addExpense(
                                  title: input.title,
                                  category: input.category,
                                  amountKes: input.amountKes,
                                  occurredAt: input.occurredAt,
                                );
                            if (context.mounted &&
                                !ref
                                    .read(expenseWriteControllerProvider)
                                    .hasError) {
                              AppFeedback.success(context, 'Transaction added',
                                  ref: ref);
                            }
                          },
                    icon: const Icon(Icons.add),
                  ),
                ),
              ],
            ),
          ),
          Expanded(
            child: AnimatedSwitcher(
              duration: contentSwitchDuration,
              switchInCurve: Curves.easeOutCubic,
              switchOutCurve: Curves.easeInCubic,
              transitionBuilder: (child, animation) =>
                  FadeTransition(opacity: animation, child: child),
              child: snapshotChild,
            ),
          ),
        ],
      ),
    );
  }

  void _consumeSearchTarget(
    BuildContext context,
    WidgetRef ref,
    ExpensesSnapshot snapshot,
  ) {
    final target = ref.read(globalSearchDeepLinkTargetProvider);
    if (target?.kind != GlobalSearchKind.expense) {
      return;
    }

    ref.read(globalSearchDeepLinkTargetProvider.notifier).state = null;

    final recordId = target?.recordId;
    if (recordId == null) {
      return;
    }

    final expense =
        snapshot.transactions.where((item) => item.id == recordId).firstOrNull;
    if (expense == null) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (context.mounted) {
          AppFeedback.info(context, 'This expense record no longer exists.',
              ref: ref);
        }
      });
      return;
    }

    ref.read(expenseFilterProvider.notifier).state = ExpenseFilter.all;
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      if (!context.mounted) {
        return;
      }
      await _editExpense(context, ref, expense);
    });
  }

  Future<void> _editExpense(
    BuildContext context,
    WidgetRef ref,
    ExpenseItem expense,
  ) async {
    final updated = await showEditExpenseDialog(context, expense: expense);
    if (updated == null) {
      return;
    }
    await ref.read(expenseWriteControllerProvider.notifier).updateExpense(
          transactionId: expense.id,
          title: updated.title,
          category: updated.category,
          amountKes: updated.amountKes,
          occurredAt: updated.occurredAt,
        );
    if (context.mounted && !ref.read(expenseWriteControllerProvider).hasError) {
      AppFeedback.success(context, 'Transaction updated', ref: ref);
    }
  }
}
