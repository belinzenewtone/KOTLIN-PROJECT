part of 'data_mode_migration_service.dart';

class _MigrationPayload {
  const _MigrationPayload({
    required this.transactions,
    required this.incomes,
    required this.tasks,
    required this.events,
    required this.budgets,
    required this.recurring,
  });

  final List<Map<String, dynamic>> transactions;
  final List<Map<String, dynamic>> incomes;
  final List<Map<String, dynamic>> tasks;
  final List<Map<String, dynamic>> events;
  final List<Map<String, dynamic>> budgets;
  final List<Map<String, dynamic>> recurring;
}
