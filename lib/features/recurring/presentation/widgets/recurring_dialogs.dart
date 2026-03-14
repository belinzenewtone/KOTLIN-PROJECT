import 'package:beltech/core/widgets/app_dialog.dart';
import 'package:beltech/features/recurring/domain/entities/recurring_template.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

class RecurringTemplateInput {
  const RecurringTemplateInput({
    required this.kind,
    required this.title,
    this.description,
    this.category,
    this.amountKes,
    this.priority,
    required this.cadence,
    required this.nextRunAt,
  });

  final RecurringKind kind;
  final String title;
  final String? description;
  final String? category;
  final double? amountKes;
  final String? priority;
  final RecurringCadence cadence;
  final DateTime nextRunAt;
}

Future<RecurringTemplateInput?> showRecurringTemplateDialog(
  BuildContext context, {
  RecurringTemplate? initial,
}) async {
  final isEdit = initial != null;
  final titleController =
      TextEditingController(text: initial?.title ?? '');
  final descriptionController =
      TextEditingController(text: initial?.description ?? '');
  final categoryController =
      TextEditingController(text: initial?.category ?? '');
  final amountController = TextEditingController(
    text: initial?.amountKes != null
        ? initial!.amountKes!.toStringAsFixed(2)
        : '',
  );
  final formKey = GlobalKey<FormState>();
  var kind = initial?.kind ?? RecurringKind.expense;
  var cadence = initial?.cadence ?? RecurringCadence.monthly;
  var priority = initial?.priority ?? 'medium';
  var nextRunAt =
      initial?.nextRunAt ?? DateTime.now().add(const Duration(hours: 1));

  final result = await showAppDialog<RecurringTemplateInput>(
    context: context,
    builder: (context) => StatefulBuilder(
      builder: (context, setState) {
        final needsAmount =
            kind == RecurringKind.expense || kind == RecurringKind.income;
        final needsCategory = kind == RecurringKind.expense;
        final needsPriority = kind == RecurringKind.task;
        return AlertDialog(
          title: Text(isEdit ? 'Edit Recurring Item' : 'New Recurring Item'),
          content: Form(
            key: formKey,
            child: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  DropdownButtonFormField<RecurringKind>(
                    initialValue: kind,
                    decoration: const InputDecoration(labelText: 'Type'),
                    items: RecurringKind.values
                        .map(
                          (value) => DropdownMenuItem(
                            value: value,
                            child: Text(value.name),
                          ),
                        )
                        .toList(),
                    onChanged: (value) {
                      if (value != null) {
                        setState(() {
                          kind = value;
                        });
                      }
                    },
                  ),
                  const SizedBox(height: 10),
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
                    controller: descriptionController,
                    decoration: const InputDecoration(
                        labelText: 'Description (optional)'),
                  ),
                  if (needsCategory) ...[
                    const SizedBox(height: 10),
                    TextFormField(
                      controller: categoryController,
                      decoration: const InputDecoration(labelText: 'Category'),
                    ),
                  ],
                  if (needsAmount) ...[
                    const SizedBox(height: 10),
                    TextFormField(
                      controller: amountController,
                      keyboardType:
                          const TextInputType.numberWithOptions(decimal: true),
                      decoration:
                          const InputDecoration(labelText: 'Amount (KES)'),
                      validator: (value) {
                        final amount = double.tryParse(value ?? '');
                        if (amount == null || amount <= 0) {
                          return 'Enter valid amount';
                        }
                        return null;
                      },
                    ),
                  ],
                  if (needsPriority) ...[
                    const SizedBox(height: 10),
                    DropdownButtonFormField<String>(
                      initialValue: priority,
                      decoration: const InputDecoration(labelText: 'Priority'),
                      items: const [
                        DropdownMenuItem(value: 'low', child: Text('Neutral')),
                        DropdownMenuItem(
                            value: 'medium', child: Text('Important')),
                        DropdownMenuItem(value: 'high', child: Text('Urgent')),
                      ],
                      onChanged: (value) {
                        if (value != null) {
                          setState(() {
                            priority = value;
                          });
                        }
                      },
                    ),
                  ],
                  const SizedBox(height: 10),
                  DropdownButtonFormField<RecurringCadence>(
                    initialValue: cadence,
                    decoration: const InputDecoration(labelText: 'Repeat'),
                    items: RecurringCadence.values
                        .map(
                          (value) => DropdownMenuItem(
                            value: value,
                            child: Text(value.name),
                          ),
                        )
                        .toList(),
                    onChanged: (value) {
                      if (value != null) {
                        setState(() {
                          cadence = value;
                        });
                      }
                    },
                  ),
                  const SizedBox(height: 10),
                  ListTile(
                    contentPadding: EdgeInsets.zero,
                    leading: const Icon(Icons.schedule),
                    title: const Text('First run'),
                    subtitle: Text(
                      DateFormat('MMM d, yyyy HH:mm').format(nextRunAt),
                    ),
                    onTap: () async {
                      final pickedDate = await showDatePicker(
                        context: context,
                        firstDate:
                            DateTime.now().subtract(const Duration(days: 365)),
                        lastDate:
                            DateTime.now().add(const Duration(days: 3650)),
                        initialDate: nextRunAt,
                      );
                      if (pickedDate == null) {
                        return;
                      }
                      if (!context.mounted) {
                        return;
                      }
                      final pickedTime = await showTimePicker(
                        context: context,
                        initialTime: TimeOfDay.fromDateTime(nextRunAt),
                      );
                      if (pickedTime == null) {
                        return;
                      }
                      setState(() {
                        nextRunAt = DateTime(
                          pickedDate.year,
                          pickedDate.month,
                          pickedDate.day,
                          pickedTime.hour,
                          pickedTime.minute,
                        );
                      });
                    },
                  ),
                ],
              ),
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
                final parsedAmount = amountController.text.trim().isEmpty
                    ? null
                    : double.tryParse(amountController.text.trim());
                Navigator.of(context).pop(
                  RecurringTemplateInput(
                    kind: kind,
                    title: titleController.text.trim(),
                    description: descriptionController.text.trim().isEmpty
                        ? null
                        : descriptionController.text.trim(),
                    category: categoryController.text.trim().isEmpty
                        ? null
                        : categoryController.text.trim(),
                    amountKes: parsedAmount,
                    priority: needsPriority ? priority : null,
                    cadence: cadence,
                    nextRunAt: nextRunAt,
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
  descriptionController.dispose();
  categoryController.dispose();
  amountController.dispose();
  return result;
}
