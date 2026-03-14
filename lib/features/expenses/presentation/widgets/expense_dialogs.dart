import 'package:beltech/core/theme/app_colors.dart';
import 'package:beltech/core/utils/currency_formatter.dart';
import 'package:beltech/core/widgets/app_dialog.dart';
import 'package:beltech/features/expenses/domain/entities/expense_item.dart';
import 'package:flutter/material.dart';

class ManualExpenseInput {
  const ManualExpenseInput({
    required this.title,
    required this.category,
    required this.amountKes,
    required this.occurredAt,
  });

  final String title;
  final String category;
  final double amountKes;
  final DateTime occurredAt;
}

Future<ManualExpenseInput?> showAddExpenseDialog(BuildContext context) async {
  return _showExpenseDialog(context);
}

Future<ManualExpenseInput?> showEditExpenseDialog(
  BuildContext context, {
  required ExpenseItem expense,
}) {
  return _showExpenseDialog(context, initialExpense: expense);
}

Future<ManualExpenseInput?> _showExpenseDialog(
  BuildContext context, {
  ExpenseItem? initialExpense,
}) async {
  final titleController =
      TextEditingController(text: initialExpense?.title ?? '');
  final amountController = TextEditingController(
      text: initialExpense == null
          ? ''
          : initialExpense.amountKes.toStringAsFixed(2));
  final categories = ['Other', 'Food', 'Transport', 'Airtime', 'Bills'];
  var selectedCategory = initialExpense == null
      ? categories.first
      : (categories.contains(initialExpense.category)
          ? initialExpense.category
          : 'Other');

  return showAppDialog<ManualExpenseInput>(
    context: context,
    builder: (context) => StatefulBuilder(
      builder: (context, setState) => AlertDialog(
        backgroundColor: AppColors.surface,
        title: Text(
            initialExpense == null ? 'Add Transaction' : 'Edit Transaction'),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: titleController,
                decoration: const InputDecoration(
                  labelText: 'Title',
                  hintText: 'e.g. Delitos Hotel',
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: amountController,
                keyboardType:
                    const TextInputType.numberWithOptions(decimal: true),
                decoration: const InputDecoration(
                  labelText: 'Amount (${CurrencyFormatter.defaultSymbol})',
                ),
              ),
              const SizedBox(height: 12),
              DropdownButtonFormField<String>(
                initialValue: selectedCategory,
                decoration: const InputDecoration(labelText: 'Category'),
                items: categories
                    .map((item) =>
                        DropdownMenuItem(value: item, child: Text(item)))
                    .toList(),
                onChanged: (value) {
                  if (value != null) {
                    setState(() => selectedCategory = value);
                  }
                },
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () {
              final title = titleController.text.trim();
              final amount = double.tryParse(amountController.text.trim());
              if (title.isEmpty || amount == null || amount <= 0) {
                return;
              }
              Navigator.of(context).pop(
                ManualExpenseInput(
                  title: title,
                  category: selectedCategory,
                  amountKes: amount,
                  occurredAt: initialExpense?.occurredAt ?? DateTime.now(),
                ),
              );
            },
            child: Text(initialExpense == null ? 'Save' : 'Update'),
          ),
        ],
      ),
    ),
  );
}
