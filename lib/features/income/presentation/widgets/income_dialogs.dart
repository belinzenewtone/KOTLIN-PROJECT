import 'package:beltech/core/widgets/app_dialog.dart';
import 'package:flutter/material.dart';

class IncomeInput {
  const IncomeInput({
    required this.title,
    required this.amountKes,
    required this.receivedAt,
  });

  final String title;
  final double amountKes;
  final DateTime receivedAt;
}

Future<IncomeInput?> showIncomeDialog(
  BuildContext context, {
  String? initialTitle,
  double? initialAmount,
  DateTime? initialDate,
}) async {
  final titleController = TextEditingController(text: initialTitle ?? '');
  final amountController = TextEditingController(
    text: initialAmount == null ? '' : initialAmount.toStringAsFixed(2),
  );
  DateTime selectedDate = initialDate ?? DateTime.now();
  final formKey = GlobalKey<FormState>();

  final result = await showAppDialog<IncomeInput>(
    context: context,
    builder: (context) => StatefulBuilder(
      builder: (context, setState) {
        return AlertDialog(
          title: Text(initialTitle == null ? 'Add Income' : 'Edit Income'),
          content: Form(
            key: formKey,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextFormField(
                  controller: titleController,
                  decoration: const InputDecoration(labelText: 'Title'),
                  validator: (value) {
                    if (value == null || value.trim().isEmpty) {
                      return 'Title is required';
                    }
                    return null;
                  },
                ),
                const SizedBox(height: 10),
                TextFormField(
                  controller: amountController,
                  keyboardType:
                      const TextInputType.numberWithOptions(decimal: true),
                  decoration: const InputDecoration(labelText: 'Amount (KES)'),
                  validator: (value) {
                    final amount = double.tryParse(value ?? '');
                    if (amount == null || amount <= 0) {
                      return 'Enter a valid amount';
                    }
                    return null;
                  },
                ),
                const SizedBox(height: 10),
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: const Icon(Icons.calendar_today_outlined),
                  title: const Text('Date'),
                  subtitle: Text(
                    '${selectedDate.year}-${selectedDate.month.toString().padLeft(2, '0')}-${selectedDate.day.toString().padLeft(2, '0')}',
                  ),
                  onTap: () async {
                    final picked = await showDatePicker(
                      context: context,
                      firstDate: DateTime(2020),
                      lastDate: DateTime(2100),
                      initialDate: selectedDate,
                    );
                    if (picked == null) {
                      return;
                    }
                    setState(() {
                      selectedDate = DateTime(
                        picked.year,
                        picked.month,
                        picked.day,
                        selectedDate.hour,
                        selectedDate.minute,
                      );
                    });
                  },
                ),
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: const Icon(Icons.access_time_outlined),
                  title: const Text('Time'),
                  subtitle: Text(
                    '${selectedDate.hour.toString().padLeft(2, '0')}:${selectedDate.minute.toString().padLeft(2, '0')}',
                  ),
                  onTap: () async {
                    final picked = await showTimePicker(
                      context: context,
                      initialTime: TimeOfDay.fromDateTime(selectedDate),
                    );
                    if (picked == null) {
                      return;
                    }
                    setState(() {
                      selectedDate = DateTime(
                        selectedDate.year,
                        selectedDate.month,
                        selectedDate.day,
                        picked.hour,
                        picked.minute,
                      );
                    });
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
                  IncomeInput(
                    title: titleController.text.trim(),
                    amountKes: double.parse(amountController.text.trim()),
                    receivedAt: selectedDate,
                  ),
                );
              },
              child: const Text('Save'),
            ),
          ],
        );
      },
    ),
  );

  titleController.dispose();
  amountController.dispose();
  return result;
}
