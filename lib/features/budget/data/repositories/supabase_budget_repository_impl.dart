import 'package:beltech/data/remote/supabase/supabase_parsers.dart';
import 'package:beltech/data/remote/supabase/supabase_polling.dart';
import 'package:beltech/features/budget/domain/entities/budget_snapshot.dart';
import 'package:beltech/features/budget/domain/entities/budget_target.dart';
import 'package:beltech/features/budget/domain/repositories/budget_repository.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

class SupabaseBudgetRepositoryImpl implements BudgetRepository {
  SupabaseBudgetRepositoryImpl(this._client);

  final SupabaseClient _client;

  @override
  Stream<BudgetSnapshot> watchMonthlySnapshot(DateTime month) {
    return pollStream(() => _loadSnapshot(month));
  }

  @override
  Future<void> upsertTarget({
    required String category,
    required double monthlyLimitKes,
  }) async {
    final userId = _requireUserId();
    final existing = (await _client
        .from('budgets')
        .select('id')
        .eq('owner_id', userId)
        .ilike('category', category)
        .limit(1)) as List;
    if (existing.isEmpty) {
      await _client.from('budgets').insert({
        'owner_id': userId,
        'category': category,
        'monthly_limit': monthlyLimitKes,
      });
      return;
    }
    final first = (existing).first as Map<String, dynamic>;
    await _client
        .from('budgets')
        .update({'category': category, 'monthly_limit': monthlyLimitKes})
        .eq('owner_id', userId)
        .eq('id', parseInt(first['id']));
  }

  @override
  Future<void> deleteTarget(int targetId) {
    final userId = _requireUserId();
    return _client
        .from('budgets')
        .delete()
        .eq('owner_id', userId)
        .eq('id', targetId);
  }

  @override
  Future<List<BudgetTarget>> loadTargets() async {
    final userId = _requireUserId();
    final rows = await _client
        .from('budgets')
        .select('id,category,monthly_limit')
        .eq('owner_id', userId)
        .order('category');
    final items = (rows as List).cast<Map<String, dynamic>>();
    return items
        .map(
          (row) => BudgetTarget(
            id: parseInt(row['id']),
            category: '${row['category'] ?? ''}',
            monthlyLimitKes: parseDouble(row['monthly_limit']),
          ),
        )
        .toList();
  }

  Future<BudgetSnapshot> _loadSnapshot(DateTime month) async {
    final userId = _requireUserId();
    final monthStart = DateTime(month.year, month.month, 1);
    final monthEnd = DateTime(month.year, month.month + 1, 1);
    final targets = await loadTargets();
    final txRows = await _client
        .from('transactions')
        .select('category, amount')
        .eq('owner_id', userId)
        .gte('occurred_at', monthStart.toUtc().toIso8601String())
        .lt('occurred_at', monthEnd.toUtc().toIso8601String());
    final txItems = (txRows as List).cast<Map<String, dynamic>>();
    final spentByCategory = <String, double>{};
    for (final tx in txItems) {
      final key = '${tx['category'] ?? ''}'.toLowerCase();
      spentByCategory[key] =
          (spentByCategory[key] ?? 0) + parseDouble(tx['amount']);
    }
    final categories = targets
        .map(
          (target) => BudgetCategoryItem(
            category: target.category,
            monthlyLimitKes: target.monthlyLimitKes,
            spentKes: spentByCategory[target.category.toLowerCase()] ?? 0,
          ),
        )
        .toList()
      ..sort((a, b) => b.spentKes.compareTo(a.spentKes));
    return BudgetSnapshot(month: monthStart, items: categories);
  }

  String _requireUserId() {
    final userId = _client.auth.currentUser?.id;
    if (userId == null || userId.isEmpty) {
      throw Exception('Sign in is required.');
    }
    return userId;
  }
}
