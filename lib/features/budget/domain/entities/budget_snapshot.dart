class BudgetCategoryItem {
  const BudgetCategoryItem({
    required this.category,
    required this.monthlyLimitKes,
    required this.spentKes,
  });

  final String category;
  final double monthlyLimitKes;
  final double spentKes;

  double get remainingKes => monthlyLimitKes - spentKes;

  double get usageRatio {
    if (monthlyLimitKes <= 0) {
      return 0;
    }
    final ratio = spentKes / monthlyLimitKes;
    if (ratio < 0) {
      return 0;
    }
    if (ratio > 1) {
      return 1;
    }
    return ratio;
  }
}

class BudgetSnapshot {
  const BudgetSnapshot({
    required this.month,
    required this.items,
  });

  final DateTime month;
  final List<BudgetCategoryItem> items;

  double get totalLimitKes =>
      items.fold<double>(0, (sum, item) => sum + item.monthlyLimitKes);

  double get totalSpentKes =>
      items.fold<double>(0, (sum, item) => sum + item.spentKes);
}
