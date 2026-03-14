import 'package:beltech/data/remote/supabase/supabase_parsers.dart';
import 'package:beltech/features/search/domain/entities/global_search_result.dart';
import 'package:beltech/features/search/domain/repositories/global_search_repository.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

class SupabaseGlobalSearchRepositoryImpl implements GlobalSearchRepository {
  SupabaseGlobalSearchRepositoryImpl(this._client);

  final SupabaseClient _client;

  @override
  Future<List<GlobalSearchResult>> search(String query) async {
    final q = query.trim();
    if (q.isEmpty) {
      return const [];
    }
    final userId = _requireUserId();
    final results = <GlobalSearchResult>[];

    final txRows = await _client
        .from('transactions')
        .select('id,title,category,amount,source,source_hash,occurred_at')
        .eq('owner_id', userId)
        .or(
          'title.ilike.%$q%,category.ilike.%$q%,source.ilike.%$q%,source_hash.ilike.%$q%',
        )
        .limit(15);
    for (final row in (txRows as List).cast<Map<String, dynamic>>()) {
      results.add(
        GlobalSearchResult(
          kind: GlobalSearchKind.expense,
          primaryText: '${row['title'] ?? ''}',
          secondaryText:
              '${row['category'] ?? 'Other'} · ${row['source'] ?? 'manual'}',
          trailingText: 'KES ${parseDouble(row['amount']).toStringAsFixed(2)}',
          recordId: parseInt(row['id']),
          recordDate: row['occurred_at'] == null
              ? null
              : parseTimestamp(row['occurred_at']),
        ),
      );
    }

    final incomeRows = await _client
        .from('incomes')
        .select('id,title,amount,source,received_at')
        .eq('owner_id', userId)
        .or('title.ilike.%$q%,source.ilike.%$q%')
        .limit(15);
    for (final row in (incomeRows as List).cast<Map<String, dynamic>>()) {
      results.add(
        GlobalSearchResult(
          kind: GlobalSearchKind.income,
          primaryText: '${row['title'] ?? ''}',
          secondaryText: '${row['source'] ?? 'manual'}',
          trailingText: 'KES ${parseDouble(row['amount']).toStringAsFixed(2)}',
          recordId: parseInt(row['id']),
          recordDate: row['received_at'] == null
              ? null
              : parseTimestamp(row['received_at']),
        ),
      );
    }

    final taskRows = await _client
        .from('tasks')
        .select('id,title,description,completed,priority,due_at')
        .eq('owner_id', userId)
        .or('title.ilike.%$q%,description.ilike.%$q%,priority.ilike.%$q%')
        .limit(15);
    for (final row in (taskRows as List).cast<Map<String, dynamic>>()) {
      final description = '${row['description'] ?? ''}'.trim();
      final priority = '${row['priority'] ?? 'medium'}';
      results.add(
        GlobalSearchResult(
          kind: GlobalSearchKind.task,
          primaryText: '${row['title'] ?? ''}',
          secondaryText:
              description.isEmpty ? priority : '$description · $priority',
          trailingText: row['completed'] == true ? 'Done' : 'Pending',
          recordId: parseInt(row['id']),
          recordDate:
              row['due_at'] == null ? null : parseTimestamp(row['due_at']),
        ),
      );
    }

    final eventRows = await _client
        .from('events')
        .select('id,title,note,priority,start_at')
        .eq('owner_id', userId)
        .or('title.ilike.%$q%,note.ilike.%$q%,priority.ilike.%$q%')
        .limit(15);
    for (final row in (eventRows as List).cast<Map<String, dynamic>>()) {
      final note = '${row['note'] ?? ''}'.trim();
      final priority = '${row['priority'] ?? 'medium'}';
      results.add(
        GlobalSearchResult(
          kind: GlobalSearchKind.event,
          primaryText: '${row['title'] ?? ''}',
          secondaryText: note.isEmpty ? priority : '$note · $priority',
          trailingText: 'Event',
          recordId: parseInt(row['id']),
          recordDate:
              row['start_at'] == null ? null : parseTimestamp(row['start_at']),
        ),
      );
    }

    final budgetRows = await _client
        .from('budgets')
        .select('id,category,monthly_limit')
        .eq('owner_id', userId)
        .ilike('category', '%$q%')
        .limit(15);
    for (final row in (budgetRows as List).cast<Map<String, dynamic>>()) {
      results.add(
        GlobalSearchResult(
          kind: GlobalSearchKind.budget,
          primaryText: '${row['category'] ?? ''}',
          secondaryText: 'Monthly budget',
          trailingText:
              'KES ${parseDouble(row['monthly_limit']).toStringAsFixed(2)}',
          recordId: parseInt(row['id']),
        ),
      );
    }

    final recurringRows = await _client
        .from('recurring_templates')
        .select('id,title,kind,cadence,description,category,next_run_at')
        .eq('owner_id', userId)
        .or(
          'title.ilike.%$q%,description.ilike.%$q%,category.ilike.%$q%,kind.ilike.%$q%,cadence.ilike.%$q%',
        )
        .limit(15);
    for (final row in (recurringRows as List).cast<Map<String, dynamic>>()) {
      final meta = [
        '${row['kind'] ?? ''}',
        '${row['cadence'] ?? ''}',
        '${row['category'] ?? ''}',
      ].where((value) => value.trim().isNotEmpty).join(' · ');
      final description = '${row['description'] ?? ''}'.trim();
      results.add(
        GlobalSearchResult(
          kind: GlobalSearchKind.recurring,
          primaryText: '${row['title'] ?? ''}',
          secondaryText: meta,
          trailingText: description.isEmpty ? 'Recurring' : description,
          recordId: parseInt(row['id']),
          recordDate: row['next_run_at'] == null
              ? null
              : parseTimestamp(row['next_run_at']),
        ),
      );
    }

    return results;
  }

  String _requireUserId() {
    final userId = _client.auth.currentUser?.id;
    if (userId == null || userId.isEmpty) {
      throw Exception('Sign in is required.');
    }
    return userId;
  }
}
