enum ExportScope {
  all,
  expenses,
  incomes,
  tasks,
  events,
  budgets,
  recurring,
}

class ExportResult {
  const ExportResult({
    required this.filePath,
    required this.rowsExported,
    required this.scope,
  });

  final String filePath;
  final int rowsExported;
  final ExportScope scope;
}
