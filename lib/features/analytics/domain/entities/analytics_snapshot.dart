class AnalyticsSnapshot {
  const AnalyticsSnapshot({
    required this.totalSpentThisMonthKes,
    required this.averageDailySpendingKes,
    required this.totalTasksCompleted,
    required this.totalTasksPending,
    required this.totalEventsThisMonth,
    required this.productivityScore,
    required this.weeklySpending,
    required this.monthlySpending,
    required this.categoryBreakdown,
  });

  final double totalSpentThisMonthKes;
  final double averageDailySpendingKes;
  final int totalTasksCompleted;
  final int totalTasksPending;
  final int totalEventsThisMonth;
  final double productivityScore;
  final List<AnalyticsPoint> weeklySpending;
  final List<AnalyticsPoint> monthlySpending;
  final List<AnalyticsCategoryShare> categoryBreakdown;
}

class AnalyticsPoint {
  const AnalyticsPoint({
    required this.label,
    required this.amountKes,
  });

  final String label;
  final double amountKes;
}

class AnalyticsCategoryShare {
  const AnalyticsCategoryShare({
    required this.category,
    required this.totalKes,
    required this.percentage,
  });

  final String category;
  final double totalKes;
  final double percentage;
}

enum AnalyticsPeriod {
  week,
  month,
}
