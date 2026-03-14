import 'package:beltech/data/remote/supabase/supabase_parsers.dart';
import 'package:beltech/features/analytics/domain/entities/analytics_snapshot.dart';
import 'package:beltech/features/analytics/domain/repositories/analytics_repository.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

class SupabaseAnalyticsRepositoryImpl implements AnalyticsRepository {
  SupabaseAnalyticsRepositoryImpl(this._client);

  final SupabaseClient _client;

  @override
  Stream<AnalyticsSnapshot> watchSnapshot() async* {
    while (true) {
      yield await _loadSnapshot();
      await Future<void>.delayed(const Duration(seconds: 2));
    }
  }

  Future<AnalyticsSnapshot> _loadSnapshot() async {
    final userId = _requireUserId();
    final now = DateTime.now();
    final monthStart = DateTime(now.year, now.month, 1);
    final monthEnd = DateTime(now.year, now.month + 1, 1);
    final weekStart = _weekStart(now);
    final weekEnd = weekStart.add(const Duration(days: 7));

    final txRows = await _client
        .from('transactions')
        .select('amount,category,occurred_at')
        .eq('owner_id', userId)
        .gte('occurred_at', monthStart.toUtc().toIso8601String())
        .lt('occurred_at', monthEnd.toUtc().toIso8601String())
        .order('occurred_at');

    final weekRows = await _client
        .from('transactions')
        .select('amount,occurred_at')
        .eq('owner_id', userId)
        .gte('occurred_at', weekStart.toUtc().toIso8601String())
        .lt('occurred_at', weekEnd.toUtc().toIso8601String())
        .order('occurred_at');

    final taskRows =
        await _client.from('tasks').select('completed').eq('owner_id', userId);

    final eventRows = await _client
        .from('events')
        .select('id')
        .eq('owner_id', userId)
        .gte('start_at', monthStart.toUtc().toIso8601String())
        .lt('start_at', monthEnd.toUtc().toIso8601String());

    final transactions = (txRows as List).cast<Map<String, dynamic>>();
    final weekTransactions = (weekRows as List).cast<Map<String, dynamic>>();
    final tasks = (taskRows as List).cast<Map<String, dynamic>>();
    final events = (eventRows as List).cast<Map<String, dynamic>>();

    final monthTotal = transactions.fold<double>(
      0,
      (sum, row) => sum + parseDouble(row['amount']),
    );
    final elapsedDays = now.day <= 0 ? 1 : now.day;
    final averageDaily = monthTotal / elapsedDays;

    final completed = tasks.where((row) => row['completed'] == true).length;
    final pending = tasks.length - completed;
    final productivity =
        _productivityScore(completed: completed, pending: pending);

    final weeklyMap = {
      for (final date in _weekDates(weekStart)) _dayShort(date): 0.0,
    };
    for (final row in weekTransactions) {
      final occurredAt = parseTimestamp(row['occurred_at']);
      final key = _dayShort(occurredAt);
      weeklyMap[key] = (weeklyMap[key] ?? 0) + parseDouble(row['amount']);
    }

    final monthDailyMap = {
      for (var day = 1;
          day <= monthEnd.subtract(const Duration(days: 1)).day;
          day++)
        '$day': 0.0,
    };
    final categoryTotals = <String, double>{};
    for (final row in transactions) {
      final amount = parseDouble(row['amount']);
      final occurredAt = parseTimestamp(row['occurred_at']);
      monthDailyMap['${occurredAt.day}'] =
          (monthDailyMap['${occurredAt.day}'] ?? 0) + amount;

      final category = '${row['category'] ?? 'Other'}';
      categoryTotals[category] = (categoryTotals[category] ?? 0) + amount;
    }

    final categoryBreakdown = categoryTotals.entries
        .map(
          (entry) => AnalyticsCategoryShare(
            category: entry.key,
            totalKes: entry.value,
            percentage: monthTotal <= 0 ? 0 : (entry.value / monthTotal) * 100,
          ),
        )
        .toList()
      ..sort((a, b) => b.totalKes.compareTo(a.totalKes));

    return AnalyticsSnapshot(
      totalSpentThisMonthKes: monthTotal,
      averageDailySpendingKes: averageDaily,
      totalTasksCompleted: completed,
      totalTasksPending: pending,
      totalEventsThisMonth: events.length,
      productivityScore: productivity,
      weeklySpending: weeklyMap.entries
          .map((entry) =>
              AnalyticsPoint(label: entry.key, amountKes: entry.value))
          .toList(),
      monthlySpending: monthDailyMap.entries
          .map((entry) =>
              AnalyticsPoint(label: entry.key, amountKes: entry.value))
          .toList(),
      categoryBreakdown: categoryBreakdown,
    );
  }

  DateTime _weekStart(DateTime date) {
    final dayStart = DateTime(date.year, date.month, date.day);
    return dayStart.subtract(Duration(days: dayStart.weekday - 1));
  }

  List<DateTime> _weekDates(DateTime weekStart) {
    return List<DateTime>.generate(
      7,
      (index) => weekStart.add(Duration(days: index)),
    );
  }

  String _dayShort(DateTime date) {
    return const {
      DateTime.monday: 'Mon',
      DateTime.tuesday: 'Tue',
      DateTime.wednesday: 'Wed',
      DateTime.thursday: 'Thu',
      DateTime.friday: 'Fri',
      DateTime.saturday: 'Sat',
      DateTime.sunday: 'Sun',
    }[date.weekday]!;
  }

  double _productivityScore({
    required int completed,
    required int pending,
  }) {
    final total = completed + pending;
    if (total <= 0) {
      return 0;
    }
    return (completed / total) * 100;
  }

  String _requireUserId() {
    final userId = _client.auth.currentUser?.id;
    if (userId == null || userId.isEmpty) {
      throw Exception('Sign in is required.');
    }
    return userId;
  }
}
