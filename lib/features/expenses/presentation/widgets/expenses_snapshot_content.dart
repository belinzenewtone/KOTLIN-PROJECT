import 'package:beltech/core/theme/app_colors.dart';
import 'package:beltech/core/utils/currency_formatter.dart';
import 'package:beltech/core/widgets/category_chip.dart';
import 'package:beltech/core/widgets/glass_card.dart';
import 'package:beltech/features/expenses/domain/entities/expense_item.dart';
import 'package:beltech/features/expenses/presentation/providers/expenses_providers.dart';
import 'package:beltech/features/expenses/presentation/widgets/transaction_row.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

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

class _SummaryCard extends StatelessWidget {
  const _SummaryCard({
    required this.title,
    required this.amount,
    this.tone = GlassCardTone.standard,
    this.accentColor,
  });

  final String title;
  final String amount;
  final GlassCardTone tone;
  final Color? accentColor;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    return GlassCard(
      tone: tone,
      accentColor: accentColor,
      child: SizedBox(
        height: 72,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(title, style: textTheme.bodyMedium),
            const SizedBox(height: 6),
            Expanded(
              child: Align(
                alignment: Alignment.centerLeft,
                child: FittedBox(
                  fit: BoxFit.scaleDown,
                  alignment: Alignment.centerLeft,
                  child: Text(
                    amount,
                    style: textTheme.titleMedium,
                    maxLines: 1,
                    softWrap: false,
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _CategoryCard extends StatelessWidget {
  const _CategoryCard({required this.categories});

  final List<CategoryExpenseTotal> categories;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final total =
        categories.fold<double>(0, (sum, item) => sum + item.totalKes);
    return GlassCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Categories', style: textTheme.titleMedium),
          const SizedBox(height: 12),
          for (final entry in categories.take(8)) ...[
            _CategoryRow(
              name: entry.category,
              amount: CurrencyFormatter.money(entry.totalKes),
              ratio: total <= 0 ? 0 : entry.totalKes / total,
            ),
            const SizedBox(height: 10),
          ],
        ],
      ),
    );
  }
}

class _CategoryRow extends StatelessWidget {
  const _CategoryRow({
    required this.name,
    required this.amount,
    required this.ratio,
  });

  final String name;
  final String amount;
  final double ratio;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final visual = _categoryVisual(name);
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        CircleAvatar(
          radius: 16,
          backgroundColor: visual.background,
          child: Icon(visual.icon, color: visual.foreground, size: 16),
        ),
        const SizedBox(width: 10),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                name,
                style: textTheme.bodyLarge,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
              const SizedBox(height: 6),
              ClipRRect(
                borderRadius: BorderRadius.circular(100),
                child: SizedBox(
                  height: 4,
                  child: LinearProgressIndicator(
                    value: ratio.clamp(0, 1),
                    backgroundColor:
                        AppColors.surfaceMuted.withValues(alpha: 0.7),
                    valueColor:
                        AlwaysStoppedAnimation<Color>(visual.foreground),
                  ),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(width: 10),
        Text(
          amount,
          style: textTheme.bodyLarge,
          maxLines: 1,
          softWrap: false,
          overflow: TextOverflow.fade,
        ),
      ],
    );
  }
}

({IconData icon, Color foreground, Color background}) _categoryVisual(
    String category) {
  final normalized = category.trim().toLowerCase();
  if (normalized.contains('food')) {
    return (
      icon: Icons.restaurant_outlined,
      foreground: AppColors.categoryFood,
      background: AppColors.categoryFoodBg,
    );
  }
  if (normalized.contains('airtime')) {
    return (
      icon: Icons.phone_android_outlined,
      foreground: AppColors.categoryAirtime,
      background: AppColors.categoryAirtimeBg,
    );
  }
  if (normalized.contains('bill')) {
    return (
      icon: Icons.receipt_long_outlined,
      foreground: AppColors.categoryAirtime,
      background: AppColors.categoryBillBg,
    );
  }
  if (normalized.contains('transport')) {
    return (
      icon: Icons.directions_bus_outlined,
      foreground: AppColors.categoryTransport,
      background: AppColors.categoryTransportBg,
    );
  }
  return (
    icon: Icons.more_horiz,
    foreground: AppColors.textSecondary,
    background: AppColors.accentSoft,
  );
}
