import 'package:beltech/core/widgets/app_dialog.dart';
import 'package:flutter/material.dart';

class BudgetInput {
  const BudgetInput({
    required this.category,
    required this.monthlyLimitKes,
  });

  final String category;
  final double monthlyLimitKes;
}

Future<BudgetInput?> showBudgetTargetDialog(
  BuildContext context, {
  String? initialCategory,
  double? initialLimit,
}) async {
  final categoryController = TextEditingController(text: initialCategory ?? '');
  final limitController = TextEditingController(
    text: initialLimit == null ? '' : initialLimit.toStringAsFixed(2),
  );
  final formKey = GlobalKey<FormState>();

  final result = await showAppDialog<BudgetInput>(
    context: context,
    builder: (context) {
      return AlertDialog(
        title: Text(initialCategory == null ? 'New Budget' : 'Edit Budget'),
        content: Form(
          key: formKey,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextFormField(
                controller: categoryController,
                decoration: const InputDecoration(labelText: 'Category'),
                validator: (value) {
                  if (value == null || value.trim().isEmpty) {
                    return 'Category is required';
                  }
                  return null;
                },
              ),
              const SizedBox(height: 10),
              TextFormField(
                controller: limitController,
                keyboardType:
                    const TextInputType.numberWithOptions(decimal: true),
                decoration:
                    const InputDecoration(labelText: 'Monthly Limit (KES)'),
                validator: (value) {
                  final parsed = double.tryParse(value ?? '');
                  if (parsed == null || parsed <= 0) {
                    return 'Enter a valid amount';
                  }
                  return null;
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
              if (formKey.currentState?.validate() != true) {
                return;
              }
              Navigator.of(context).pop(
                BudgetInput(
                  category: categoryController.text.trim(),
                  monthlyLimitKes: double.parse(limitController.text.trim()),
                ),
              );
            },
            child: const Text('Save'),
          ),
        ],
      );
    },
  );

  categoryController.dispose();
  limitController.dispose();
  return result;
}
