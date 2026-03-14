import 'package:beltech/core/theme/app_colors.dart';
import 'package:beltech/core/utils/currency_formatter.dart';
import 'package:beltech/core/widgets/category_chip.dart';
import 'package:beltech/core/widgets/glass_card.dart';
import 'package:beltech/features/expenses/domain/entities/expense_item.dart';
import 'package:beltech/features/expenses/presentation/providers/expenses_providers.dart';
import 'package:beltech/features/expenses/presentation/widgets/transaction_row.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

part 'expenses_snapshot_content_cards.dart';

final _txDateFormat = DateFormat('MMM d, HH:mm');

class ExpensesSnapshotContent extends StatefulWidget {
  const ExpensesSnapshotContent({
    super.key,
    required this.snapshot,
    required this.selectedFilter,
    required this.busy,
    required this.onFilterChanged,
    required this.onEditExpense,
    required this.onDeleteExpense,
  });

  final ExpensesSnapshot snapshot;
  final ExpenseFilter selectedFilter;
  final bool busy;
  final ValueChanged<ExpenseFilter> onFilterChanged;
  final ValueChanged<ExpenseItem> onEditExpense;
  final ValueChanged<ExpenseItem> onDeleteExpense;

  @override
  State<ExpensesSnapshotContent> createState() =>
      _ExpensesSnapshotContentState();
}

class _ExpensesSnapshotContentState extends State<ExpensesSnapshotContent> {
  bool _showAllTransactions = false;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final transactions = _transactionsForFilter(
        widget.snapshot.transactions, widget.selectedFilter);
    final visibleTransactions =
        _showAllTransactions ? transactions : transactions.take(20).toList();
    return ListView(
      padding: const EdgeInsets.only(bottom: 24),
      children: [
        Row(
          children: [
            Expanded(
              child: _SummaryCard(
                title: 'Today',
                amount: CurrencyFormatter.money(widget.snapshot.todayKes),
                tone: GlassCardTone.accent,
                accentColor: AppColors.accent,
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: _SummaryCard(
                title: 'This Week',
                amount: CurrencyFormatter.money(widget.snapshot.weekKes),
                tone: GlassCardTone.accent,
                accentColor: AppColors.teal,
              ),
            ),
          ],
        ),
        const SizedBox(height: 14),
        SizedBox(
          height: 42,
          child: ListView(
            scrollDirection: Axis.horizontal,
            children: ExpenseFilter.values.map((filter) {
              return Padding(
                padding: const EdgeInsets.only(right: 8),
                child: CategoryChip(
                  label: switch (filter) {
                    ExpenseFilter.all => 'All',
                    ExpenseFilter.today => 'Today',
                    ExpenseFilter.week => 'This Week',
                    ExpenseFilter.month => 'This Month',
                  },
                  selected: widget.selectedFilter == filter,
                  onTap: () => widget.onFilterChanged(filter),
                ),
              );
            }).toList(),
          ),
        ),
        const SizedBox(height: 14),
        _CategoryCard(categories: widget.snapshot.categories),
        const SizedBox(height: 16),
        Text('Transactions', style: textTheme.titleMedium),
        const SizedBox(height: 10),
        for (final tx in visibleTransactions) ...[
          ExpenseTransactionRow(
            dismissKey: 'expense-${tx.id}',
            title: tx.title,
            subtitle: '${tx.category} · ${_txDateFormat.format(tx.occurredAt)}',
            amount: CurrencyFormatter.money(tx.amountKes),
            onEdit: () => widget.onEditExpense(tx),
            onDelete: () => widget.onDeleteExpense(tx),
            busy: widget.busy,
          ),
          const SizedBox(height: 10),
        ],
        if (transactions.length > 20)
          Align(
            alignment: Alignment.centerLeft,
            child: TextButton(
              onPressed: () {
                setState(() {
                  _showAllTransactions = !_showAllTransactions;
                });
              },
              child: Text(_showAllTransactions
                  ? 'Show fewer transactions'
                  : 'Show all transactions (${transactions.length})'),
            ),
          ),
      ],
    );
  }

  List<ExpenseItem> _transactionsForFilter(
      List<ExpenseItem> source, ExpenseFilter filter) {
    final now = DateTime.now();
    final dayStart = DateTime(now.year, now.month, now.day);
    final weekStart = dayStart.subtract(Duration(days: now.weekday - 1));
    final monthStart = DateTime(now.year, now.month, 1);
    return source.where((item) {
      switch (filter) {
        case ExpenseFilter.today:
          return !item.occurredAt.isBefore(dayStart);
        case ExpenseFilter.week:
          return !item.occurredAt.isBefore(weekStart);
        case ExpenseFilter.month:
          return !item.occurredAt.isBefore(monthStart);
        case ExpenseFilter.all:
          return true;
      }
    }).toList();
  }
}
