import 'package:beltech/data/remote/supabase/supabase_parsers.dart';
import 'package:beltech/data/remote/supabase/supabase_polling.dart';
import 'package:beltech/features/home/domain/entities/home_overview.dart';
import 'package:beltech/features/home/domain/repositories/home_repository.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

class SupabaseHomeRepositoryImpl implements HomeRepository {
  SupabaseHomeRepositoryImpl(this._client);

  final SupabaseClient _client;

  @override
  Stream<HomeOverview> watchOverview() => pollStream(_loadOverview);

  Future<HomeOverview> _loadOverview() async {
    final userId = _requireUserId();
    final now = DateTime.now();
    final todayStart = DateTime(now.year, now.month, now.day);
    final tomorrowStart = todayStart.add(const Duration(days: 1));
    final weekStart = todayStart.subtract(Duration(days: now.weekday - 1));
    final weekEnd = weekStart.add(const Duration(days: 7));

    final txRows = await _client
        .from('transactions')
        .select('id,title,category,amount,occurred_at')
        .eq('owner_id', userId)
        .order('occurred_at', ascending: false)
        .limit(5000);
    final transactions = (txRows as List).cast<Map<String, dynamic>>();

    final todayKes = _sumBetween(transactions, todayStart, tomorrowStart);
    final weekKes = _sumBetween(transactions, weekStart, weekEnd);
    final weekly = _weeklyMap(transactions, now);
    final recent = transactions.take(5).map((row) {
      return HomeTransaction(
        title: '${row['title'] ?? ''}',
        category: '${row['category'] ?? 'Other'}',
        amountKes: parseDouble(row['amount']),
      );
    }).toList();

    final taskRows =
        await _client.from('tasks').select('completed').eq('owner_id', userId);
    final tasks = (taskRows as List).cast<Map<String, dynamic>>();
    final completed = tasks.where((row) => row['completed'] == true).length;
    final pending = tasks.length - completed;

    final eventRows = await _client
        .from('events')
        .select('id')
        .eq('owner_id', userId)
        .gte('start_at', todayStart.toIso8601String());
    final upcoming = (eventRows as List).length;

    return HomeOverview(
      todayKes: todayKes,
      weekKes: weekKes,
      completedCount: completed,
      pendingCount: pending,
      upcomingEventsCount: upcoming,
      weeklySpendingKes: weekly,
      recentTransactions: recent,
    );
  }

  double _sumBetween(
      List<Map<String, dynamic>> rows, DateTime start, DateTime end) {
    var total = 0.0;
    for (final row in rows) {
      final at = parseTimestamp(row['occurred_at']);
      if (!at.isBefore(start) && at.isBefore(end)) {
        total += parseDouble(row['amount']);
      }
    }
    return total;
  }

  Map<String, double> _weeklyMap(
      List<Map<String, dynamic>> rows, DateTime now) {
    const labels = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
    final weekStart = DateTime(now.year, now.month, now.day)
        .subtract(Duration(days: now.weekday % 7));
    final result = {for (final label in labels) label: 0.0};
    for (final row in rows) {
      final at = parseTimestamp(row['occurred_at']);
      final dayStart = DateTime(at.year, at.month, at.day);
      final index = dayStart.difference(weekStart).inDays;
      if (index >= 0 && index < 7) {
        result[labels[index]] =
            (result[labels[index]] ?? 0) + parseDouble(row['amount']);
      }
    }
    return result;
  }

  String _requireUserId() {
    final userId = _client.auth.currentUser?.id;
    if (userId == null || userId.isEmpty) {
      throw Exception('Sign in is required.');
    }
    return userId;
  }
}
