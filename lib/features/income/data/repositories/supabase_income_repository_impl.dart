import 'package:beltech/data/remote/supabase/supabase_parsers.dart';
import 'package:beltech/data/remote/supabase/supabase_polling.dart';
import 'package:beltech/features/income/domain/entities/income_item.dart';
import 'package:beltech/features/income/domain/repositories/income_repository.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

class SupabaseIncomeRepositoryImpl implements IncomeRepository {
  SupabaseIncomeRepositoryImpl(this._client);

  final SupabaseClient _client;

  @override
  Stream<List<IncomeItem>> watchIncomes() => pollStream(_loadIncomes);

  @override
  Future<void> addIncome({
    required String title,
    required double amountKes,
    DateTime? receivedAt,
    String source = 'manual',
  }) {
    final userId = _requireUserId();
    return _client.from('incomes').insert({
      'owner_id': userId,
      'title': title,
      'amount': amountKes,
      'received_at': (receivedAt ?? DateTime.now()).toUtc().toIso8601String(),
      'source': source,
    });
  }

  @override
  Future<void> updateIncome({
    required int incomeId,
    required String title,
    required double amountKes,
    required DateTime receivedAt,
  }) {
    final userId = _requireUserId();
    return _client
        .from('incomes')
        .update({
          'title': title,
          'amount': amountKes,
          'received_at': receivedAt.toUtc().toIso8601String(),
          'source': 'manual',
        })
        .eq('owner_id', userId)
        .eq('id', incomeId);
  }

  @override
  Future<void> deleteIncome(int incomeId) {
    final userId = _requireUserId();
    return _client
        .from('incomes')
        .delete()
        .eq('owner_id', userId)
        .eq('id', incomeId);
  }

  Future<List<IncomeItem>> _loadIncomes() async {
    final userId = _requireUserId();
    final rows = await _client
        .from('incomes')
        .select('id,title,amount,received_at,source')
        .eq('owner_id', userId)
        .order('received_at', ascending: false);
    final items = (rows as List).cast<Map<String, dynamic>>();
    return items
        .map(
          (row) => IncomeItem(
            id: parseInt(row['id']),
            title: '${row['title'] ?? ''}',
            amountKes: parseDouble(row['amount']),
            receivedAt: parseTimestamp(row['received_at']),
            source: '${row['source'] ?? 'manual'}',
          ),
        )
        .toList();
  }

  String _requireUserId() {
    final userId = _client.auth.currentUser?.id;
    if (userId == null || userId.isEmpty) {
      throw Exception('Sign in is required.');
    }
    return userId;
  }
}
