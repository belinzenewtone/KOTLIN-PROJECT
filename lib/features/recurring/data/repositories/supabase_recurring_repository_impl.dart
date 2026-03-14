import 'package:beltech/data/remote/supabase/supabase_parsers.dart';
import 'package:beltech/data/remote/supabase/supabase_polling.dart';
import 'package:beltech/features/recurring/domain/entities/recurring_template.dart';
import 'package:beltech/features/recurring/domain/repositories/recurring_repository.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

class SupabaseRecurringRepositoryImpl implements RecurringRepository {
  SupabaseRecurringRepositoryImpl(this._client);

  final SupabaseClient _client;

  @override
  Stream<List<RecurringTemplate>> watchTemplates() =>
      pollStream(_loadTemplates);

  @override
  Future<void> addTemplate({
    required RecurringKind kind,
    required String title,
    String? description,
    String? category,
    double? amountKes,
    String? priority,
    required RecurringCadence cadence,
    required DateTime nextRunAt,
    bool enabled = true,
  }) {
    final userId = _requireUserId();
    return _client.from('recurring_templates').insert({
      'owner_id': userId,
      'kind': kind.name,
      'title': title,
      'description': description,
      'category': category,
      'amount': amountKes,
      'priority': priority,
      'cadence': cadence.name,
      'next_run_at': nextRunAt.toUtc().toIso8601String(),
      'enabled': enabled,
    });
  }

  @override
  Future<void> updateTemplate({
    required int templateId,
    required RecurringKind kind,
    required String title,
    String? description,
    String? category,
    double? amountKes,
    String? priority,
    required RecurringCadence cadence,
    required DateTime nextRunAt,
    required bool enabled,
  }) {
    final userId = _requireUserId();
    return _client
        .from('recurring_templates')
        .update({
          'kind': kind.name,
          'title': title,
          'description': description,
          'category': category,
          'amount': amountKes,
          'priority': priority,
          'cadence': cadence.name,
          'next_run_at': nextRunAt.toUtc().toIso8601String(),
          'enabled': enabled,
        })
        .eq('owner_id', userId)
        .eq('id', templateId);
  }

  @override
  Future<void> deleteTemplate(int templateId) {
    final userId = _requireUserId();
    return _client
        .from('recurring_templates')
        .delete()
        .eq('owner_id', userId)
        .eq('id', templateId);
  }

  @override
  Future<int> materializeDue({DateTime? now}) async {
    final userId = _requireUserId();
    final clock = (now ?? DateTime.now()).toUtc();
    final rows = await _client
        .from('recurring_templates')
        .select(
            'id,kind,title,description,category,amount,priority,cadence,next_run_at')
        .eq('owner_id', userId)
        .eq('enabled', true)
        .lte('next_run_at', clock.toIso8601String())
        .order('next_run_at', ascending: true);
    final due = (rows as List).cast<Map<String, dynamic>>();
    if (due.isEmpty) {
      return 0;
    }
    var inserted = 0;
    for (final row in due) {
      final templateId = parseInt(row['id']);
      final kind = _kindFrom('${row['kind'] ?? ''}');
      final when = parseTimestamp(row['next_run_at']).toUtc();
      final title = '${row['title'] ?? ''}';
      final description = row['description'] as String?;
      final category = row['category'] as String?;
      final amount = parseDouble(row['amount']);
      final priority = row['priority'] as String?;
      switch (kind) {
        case RecurringKind.expense:
          await _client.from('transactions').insert({
            'owner_id': userId,
            'title': title,
            'category': category ?? 'Other',
            'amount': amount,
            'occurred_at': when.toIso8601String(),
            'source': 'recurring',
            'source_hash': null,
          });
          inserted += 1;
        case RecurringKind.income:
          await _client.from('incomes').insert({
            'owner_id': userId,
            'title': title,
            'amount': amount,
            'received_at': when.toIso8601String(),
            'source': 'recurring',
          });
          inserted += 1;
        case RecurringKind.task:
          await _client.from('tasks').insert({
            'owner_id': userId,
            'title': title,
            'description': description,
            'completed': false,
            'due_at': when.toIso8601String(),
            'priority': priority ?? 'medium',
          });
          inserted += 1;
        case RecurringKind.event:
          await _client.from('events').insert({
            'owner_id': userId,
            'title': title,
            'start_at': when.toIso8601String(),
            'end_at': null,
            'note': description,
          });
          inserted += 1;
      }
      final cadence = _cadenceFrom('${row['cadence'] ?? ''}');
      final nextRun = _nextRun(when.toLocal(), cadence).toUtc();
      await _client
          .from('recurring_templates')
          .update({'next_run_at': nextRun.toIso8601String()})
          .eq('owner_id', userId)
          .eq('id', templateId);
    }
    return inserted;
  }

  Future<List<RecurringTemplate>> _loadTemplates() async {
    final userId = _requireUserId();
    final rows = await _client
        .from('recurring_templates')
        .select(
            'id,kind,title,description,category,amount,priority,cadence,next_run_at,enabled')
        .eq('owner_id', userId)
        .order('next_run_at', ascending: true);
    final items = (rows as List).cast<Map<String, dynamic>>();
    return items
        .map(
          (row) => RecurringTemplate(
            id: parseInt(row['id']),
            kind: _kindFrom('${row['kind'] ?? ''}'),
            title: '${row['title'] ?? ''}',
            description: row['description'] as String?,
            category: row['category'] as String?,
            amountKes:
                row['amount'] == null ? null : parseDouble(row['amount']),
            priority: row['priority'] as String?,
            cadence: _cadenceFrom('${row['cadence'] ?? ''}'),
            nextRunAt: parseTimestamp(row['next_run_at']),
            enabled: row['enabled'] == true,
          ),
        )
        .toList();
  }

  DateTime _nextRun(DateTime from, RecurringCadence cadence) {
    return switch (cadence) {
      RecurringCadence.daily => from.add(const Duration(days: 1)),
      RecurringCadence.weekly => from.add(const Duration(days: 7)),
      RecurringCadence.monthly => DateTime(
          from.year,
          from.month + 1,
          from.day,
          from.hour,
          from.minute,
        ),
    };
  }

  RecurringKind _kindFrom(String raw) {
    return switch (raw.toLowerCase()) {
      'income' => RecurringKind.income,
      'task' => RecurringKind.task,
      'event' => RecurringKind.event,
      _ => RecurringKind.expense,
    };
  }

  RecurringCadence _cadenceFrom(String raw) {
    return switch (raw.toLowerCase()) {
      'weekly' => RecurringCadence.weekly,
      'monthly' => RecurringCadence.monthly,
      _ => RecurringCadence.daily,
    };
  }

  String _requireUserId() {
    final userId = _client.auth.currentUser?.id;
    if (userId == null || userId.isEmpty) {
      throw Exception('Sign in is required.');
    }
    return userId;
  }
}
