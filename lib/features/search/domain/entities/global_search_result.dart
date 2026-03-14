enum GlobalSearchKind {
  expense,
  income,
  task,
  event,
  budget,
  recurring,
}

class GlobalSearchResult {
  const GlobalSearchResult({
    required this.kind,
    required this.primaryText,
    required this.secondaryText,
    required this.trailingText,
    this.recordId,
    this.recordDate,
  });

  final GlobalSearchKind kind;
  final String primaryText;
  final String secondaryText;
  final String trailingText;
  final int? recordId;
  final DateTime? recordDate;
}
