import 'package:beltech/core/theme/app_spacing.dart';
import 'package:beltech/core/utils/currency_formatter.dart';
import 'package:beltech/core/widgets/app_feedback.dart';
import 'package:beltech/core/widgets/error_message.dart';
import 'package:beltech/core/widgets/glass_card.dart';
import 'package:beltech/core/widgets/loading_indicator.dart';
import 'package:intl/intl.dart';
import 'package:beltech/features/income/domain/entities/income_item.dart';
import 'package:beltech/features/income/presentation/providers/income_providers.dart';
import 'package:beltech/features/income/presentation/widgets/income_dialogs.dart';
import 'package:beltech/features/search/domain/entities/global_search_result.dart';
import 'package:beltech/features/search/presentation/providers/global_search_providers.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class IncomeScreen extends ConsumerWidget {
  const IncomeScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final incomesState = ref.watch(incomesProvider);
    final writeState = ref.watch(incomeWriteControllerProvider);
    final textTheme = Theme.of(context).textTheme;

    ref.listen<AsyncValue<void>>(incomeWriteControllerProvider,
        (previous, next) {
      if (next.hasError) {
        AppFeedback.error(context, 'Unable to save income changes.');
      } else if (previous is AsyncLoading && next is AsyncData<void>) {
        AppFeedback.success(context, 'Income changes saved successfully.');
      }
    });

    return Scaffold(
      appBar: AppBar(
        title: const Text('Income'),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: writeState.isLoading
            ? null
            : () async {
                final input = await showIncomeDialog(context);
                if (input == null) {
                  return;
                }
                await ref
                    .read(incomeWriteControllerProvider.notifier)
                    .addIncome(
                      title: input.title,
                      amountKes: input.amountKes,
                      receivedAt: input.receivedAt,
                    );
              },
        child: const Icon(Icons.add),
      ),
      body: SafeArea(
        child: Padding(
          padding: AppSpacing.sectionPadding(context),
          child: incomesState.when(
            data: (incomes) {
              _consumeSearchTarget(context, ref, incomes);
              if (incomes.isEmpty) {
                return const GlassCard(child: Text('No income records yet'));
              }
              final total =
                  incomes.fold<double>(0, (sum, item) => sum + item.amountKes);
              return Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  GlassCard(
                    child: Row(
                      children: [
                        Text('Total Income', style: textTheme.titleMedium),
                        const Spacer(),
                        Flexible(
                          child: Text(
                            CurrencyFormatter.money(total),
                            style: textTheme.titleMedium,
                            maxLines: 1,
                            softWrap: false,
                            overflow: TextOverflow.fade,
                            textAlign: TextAlign.end,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 12),
                  Expanded(
                    child: ListView.separated(
                      itemCount: incomes.length,
                      separatorBuilder: (_, __) => const SizedBox(height: 10),
                      itemBuilder: (context, index) {
                        final item = incomes[index];
                        return _IncomeRow(
                          item: item,
                          busy: writeState.isLoading,
                          onEdit: () async {
                            final input = await showIncomeDialog(
                              context,
                              initialTitle: item.title,
                              initialAmount: item.amountKes,
                              initialDate: item.receivedAt,
                            );
                            if (input == null) {
                              return;
                            }
                            await ref
                                .read(incomeWriteControllerProvider.notifier)
                                .updateIncome(
                                  incomeId: item.id,
                                  title: input.title,
                                  amountKes: input.amountKes,
                                  receivedAt: input.receivedAt,
                                );
                          },
                          onDelete: () async {
                            await ref
                                .read(incomeWriteControllerProvider.notifier)
                                .deleteIncome(item.id);
                          },
                        );
                      },
                    ),
                  ),
                ],
              );
            },
            loading: () => Center(child: LoadingIndicator()),
            error: (_, __) => ErrorMessage(
              label: 'Unable to load incomes',
              onRetry: () => ref.invalidate(incomesProvider),
            ),
          ),
        ),
      ),
    );
  }

  void _consumeSearchTarget(
    BuildContext context,
    WidgetRef ref,
    List<IncomeItem> incomes,
  ) {
    final target = ref.read(globalSearchDeepLinkTargetProvider);
    if (target?.kind != GlobalSearchKind.income) {
      return;
    }

    ref.read(globalSearchDeepLinkTargetProvider.notifier).state = null;

    final recordId = target?.recordId;
    if (recordId == null) {
      return;
    }

    final item = incomes.where((income) => income.id == recordId).firstOrNull;
    if (item == null) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (context.mounted) {
          AppFeedback.info(context, 'This income record no longer exists.');
        }
      });
      return;
    }

    WidgetsBinding.instance.addPostFrameCallback((_) async {
      if (!context.mounted) {
        return;
      }
      final input = await showIncomeDialog(
        context,
        initialTitle: item.title,
        initialAmount: item.amountKes,
        initialDate: item.receivedAt,
      );
      if (input == null) {
        return;
      }
      await ref.read(incomeWriteControllerProvider.notifier).updateIncome(
            incomeId: item.id,
            title: input.title,
            amountKes: input.amountKes,
            receivedAt: input.receivedAt,
          );
    });
  }
}

class _IncomeRow extends StatelessWidget {
  const _IncomeRow({
    required this.item,
    required this.busy,
    required this.onEdit,
    required this.onDelete,
  });

  final IncomeItem item;
  final bool busy;
  final VoidCallback onEdit;
  final VoidCallback onDelete;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    return GlassCard(
      child: Row(
        children: [
          const CircleAvatar(
            child: Icon(Icons.account_balance_wallet_outlined),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(item.title, style: textTheme.bodyLarge),
                Text(
                  DateFormat('MMM d, yyyy').format(item.receivedAt),
                  style: textTheme.bodyMedium,
                ),
              ],
            ),
          ),
          Text(
            CurrencyFormatter.money(item.amountKes),
            style: textTheme.bodyLarge,
            maxLines: 1,
            softWrap: false,
            overflow: TextOverflow.fade,
          ),
          IconButton(
            onPressed: busy ? null : onEdit,
            icon: const Icon(Icons.edit_outlined),
          ),
          IconButton(
            onPressed: busy ? null : onDelete,
            icon: const Icon(Icons.delete_outline),
          ),
        ],
      ),
    );
  }
}
