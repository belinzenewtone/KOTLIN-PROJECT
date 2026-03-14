part of 'data_mode_migration_service.dart';

class _CloudMigrationGateway {
  _CloudMigrationGateway({
    required SupabaseClient? client,
    required this.codec,
    required this.orderedScopes,
  }) : _client = client;

  final SupabaseClient? _client;
  final _MigrationCodec codec;
  final List<String> orderedScopes;

  Future<Map<String, int>> loadCounts(String userId) async {
    final client = _requireClient();
    final counts = codec.emptyScopeCounts(orderedScopes);

    final transactions =
        await client.from('transactions').select('id').eq('owner_id', userId);
    counts[DataModeMigrationService.scopeExpenses] =
        (transactions as List).length;

    final incomes =
        await client.from('incomes').select('id').eq('owner_id', userId);
    counts[DataModeMigrationService.scopeIncome] = (incomes as List).length;

    final tasks = await client.from('tasks').select('id').eq('owner_id', userId);
    counts[DataModeMigrationService.scopeTasks] = (tasks as List).length;

    final events =
        await client.from('events').select('id').eq('owner_id', userId);
    counts[DataModeMigrationService.scopeEvents] = (events as List).length;

    final budgets =
        await client.from('budgets').select('id').eq('owner_id', userId);
    counts[DataModeMigrationService.scopeBudgets] = (budgets as List).length;

    final recurring = await client
        .from('recurring_templates')
        .select('id')
        .eq('owner_id', userId);
    counts[DataModeMigrationService.scopeRecurring] = (recurring as List).length;

    return counts;
  }

  Future<_MigrationPayload> readPayload(String userId) async {
    final client = _requireClient();
    final transactions = codec.toMapList(await client
        .from('transactions')
        .select('title,category,amount,occurred_at,source,source_hash')
        .eq('owner_id', userId));
    final incomes = codec.toMapList(await client
        .from('incomes')
        .select('title,amount,received_at,source')
        .eq('owner_id', userId));
    final tasks = codec.toMapList(await client
        .from('tasks')
        .select('title,description,completed,due_at,priority')
        .eq('owner_id', userId));
    final events = codec.toMapList(await client
        .from('events')
        .select('title,start_at,end_at,note,completed,priority,event_type')
        .eq('owner_id', userId));
    final budgets = codec.toMapList(await client
        .from('budgets')
        .select('category,monthly_limit')
        .eq('owner_id', userId));
    final recurring = codec.toMapList(await client
        .from('recurring_templates')
        .select(
            'kind,title,description,category,amount,priority,cadence,next_run_at,enabled')
        .eq('owner_id', userId));

    return _MigrationPayload(
      transactions: transactions,
      incomes: incomes,
      tasks: tasks,
      events: events,
      budgets: budgets,
      recurring: recurring,
    );
  }

  Future<DataMigrationResult> writePayload(
    _MigrationPayload payload,
    String userId,
  ) async {
    final client = _requireClient();
    final inserted = codec.emptyScopeCounts(orderedScopes);
    final updated = codec.emptyScopeCounts(orderedScopes);

    final existingTransactions = codec.toMapList(await client
        .from('transactions')
        .select('title,category,amount,occurred_at,source,source_hash')
        .eq('owner_id', userId));
    final transactionSignatures =
        existingTransactions.map(codec.transactionSignature).toSet();
    final transactionInsertPayload = <Map<String, dynamic>>[];
    for (final row in payload.transactions) {
      final signature = codec.transactionSignature(row);
      if (!transactionSignatures.add(signature)) {
        continue;
      }
      transactionInsertPayload.add({
        'owner_id': userId,
        'title': codec.requiredText(row['title'], fallback: 'Untitled'),
        'category': codec.requiredText(row['category'], fallback: 'Other'),
        'amount': codec.asDouble(row['amount']),
        'occurred_at': codec.requiredIsoUtc(row['occurred_at']),
        'source': codec.requiredText(row['source'], fallback: 'manual'),
        'source_hash': codec.nullableText(row['source_hash']),
      });
    }
    if (transactionInsertPayload.isNotEmpty) {
      await client.from('transactions').insert(transactionInsertPayload);
      inserted[DataModeMigrationService.scopeExpenses] =
          transactionInsertPayload.length;
    }

    final existingIncomes = codec.toMapList(await client
        .from('incomes')
        .select('title,amount,received_at,source')
        .eq('owner_id', userId));
    final incomeSignatures = existingIncomes.map(codec.incomeSignature).toSet();
    final incomeInsertPayload = <Map<String, dynamic>>[];
    for (final row in payload.incomes) {
      final signature = codec.incomeSignature(row);
      if (!incomeSignatures.add(signature)) {
        continue;
      }
      incomeInsertPayload.add({
        'owner_id': userId,
        'title': codec.requiredText(row['title'], fallback: 'Income'),
        'amount': codec.asDouble(row['amount']),
        'received_at': codec.requiredIsoUtc(row['received_at']),
        'source': codec.requiredText(row['source'], fallback: 'manual'),
      });
    }
    if (incomeInsertPayload.isNotEmpty) {
      await client.from('incomes').insert(incomeInsertPayload);
      inserted[DataModeMigrationService.scopeIncome] = incomeInsertPayload.length;
    }

    final existingTasks = codec.toMapList(await client
        .from('tasks')
        .select('title,description,completed,due_at,priority')
        .eq('owner_id', userId));
    final taskSignatures = existingTasks.map(codec.taskSignature).toSet();
    final taskInsertPayload = <Map<String, dynamic>>[];
    for (final row in payload.tasks) {
      final signature = codec.taskSignature(row);
      if (!taskSignatures.add(signature)) {
        continue;
      }
      taskInsertPayload.add({
        'owner_id': userId,
        'title': codec.requiredText(row['title'], fallback: 'Task'),
        'description': codec.nullableText(row['description']),
        'completed': codec.asBool(row['completed']),
        'due_at': codec.nullableIsoUtc(row['due_at']),
        'priority': codec.requiredText(row['priority'], fallback: 'medium'),
      });
    }
    if (taskInsertPayload.isNotEmpty) {
      await client.from('tasks').insert(taskInsertPayload);
      inserted[DataModeMigrationService.scopeTasks] = taskInsertPayload.length;
    }

    final existingEvents = codec.toMapList(await client
        .from('events')
        .select('title,start_at,end_at,note,completed,priority,event_type')
        .eq('owner_id', userId));
    final eventSignatures = existingEvents.map(codec.eventSignature).toSet();
    final eventInsertPayload = <Map<String, dynamic>>[];
    for (final row in payload.events) {
      final signature = codec.eventSignature(row);
      if (!eventSignatures.add(signature)) {
        continue;
      }
      eventInsertPayload.add({
        'owner_id': userId,
        'title': codec.requiredText(row['title'], fallback: 'Event'),
        'start_at': codec.requiredIsoUtc(row['start_at']),
        'end_at': codec.nullableIsoUtc(row['end_at']),
        'note': codec.nullableText(row['note']),
        'completed': codec.asBool(row['completed']),
        'priority': codec.requiredText(row['priority'], fallback: 'medium'),
        'event_type': codec.requiredText(row['event_type'], fallback: 'general'),
      });
    }
    if (eventInsertPayload.isNotEmpty) {
      await client.from('events').insert(eventInsertPayload);
      inserted[DataModeMigrationService.scopeEvents] = eventInsertPayload.length;
    }

    final existingBudgets = codec.toMapList(await client
        .from('budgets')
        .select('id,category,monthly_limit')
        .eq('owner_id', userId));
    final budgetByCategory = <String, Map<String, dynamic>>{};
    for (final row in existingBudgets) {
      budgetByCategory[codec.normalized(row['category'])] = row;
    }
    final budgetInsertPayload = <Map<String, dynamic>>[];
    for (final row in payload.budgets) {
      final category = codec.requiredText(row['category'], fallback: '');
      if (category.isEmpty) {
        continue;
      }
      final key = codec.normalized(category);
      final nextLimit = codec.asDouble(row['monthly_limit']);
      final existing = budgetByCategory[key];
      if (existing == null) {
        budgetInsertPayload.add({
          'owner_id': userId,
          'category': category,
          'monthly_limit': nextLimit,
        });
        budgetByCategory[key] = {
          'category': category,
          'monthly_limit': nextLimit,
        };
        continue;
      }
      final currentLimit = codec.asDouble(existing['monthly_limit']);
      final currentCategory = codec.requiredText(existing['category'], fallback: '');
      if (codec.doubleEquals(currentLimit, nextLimit) &&
          currentCategory == category) {
        continue;
      }
      await client
          .from('budgets')
          .update({'category': category, 'monthly_limit': nextLimit})
          .eq('owner_id', userId)
          .eq('id', codec.asInt(existing['id']));
      existing['category'] = category;
      existing['monthly_limit'] = nextLimit;
      updated[DataModeMigrationService.scopeBudgets] =
          (updated[DataModeMigrationService.scopeBudgets] ?? 0) + 1;
    }
    if (budgetInsertPayload.isNotEmpty) {
      await client.from('budgets').insert(budgetInsertPayload);
      inserted[DataModeMigrationService.scopeBudgets] = budgetInsertPayload.length;
    }

    final existingRecurring = codec.toMapList(await client
        .from('recurring_templates')
        .select(
            'kind,title,description,category,amount,priority,cadence,next_run_at,enabled')
        .eq('owner_id', userId));
    final recurringSignatures =
        existingRecurring.map(codec.recurringSignature).toSet();
    final recurringInsertPayload = <Map<String, dynamic>>[];
    for (final row in payload.recurring) {
      final signature = codec.recurringSignature(row);
      if (!recurringSignatures.add(signature)) {
        continue;
      }
      recurringInsertPayload.add({
        'owner_id': userId,
        'kind': codec.requiredText(row['kind'], fallback: 'expense'),
        'title': codec.requiredText(row['title'], fallback: 'Template'),
        'description': codec.nullableText(row['description']),
        'category': codec.nullableText(row['category']),
        'amount': codec.nullableDouble(row['amount']),
        'priority': codec.nullableText(row['priority']),
        'cadence': codec.requiredText(row['cadence'], fallback: 'daily'),
        'next_run_at': codec.requiredIsoUtc(row['next_run_at']),
        'enabled': codec.asBool(row['enabled']),
      });
    }
    if (recurringInsertPayload.isNotEmpty) {
      await client.from('recurring_templates').insert(recurringInsertPayload);
      inserted[DataModeMigrationService.scopeRecurring] =
          recurringInsertPayload.length;
    }

    return DataMigrationResult(insertedCounts: inserted, updatedCounts: updated);
  }

  SupabaseClient _requireClient() {
    final client = _client;
    if (client == null) {
      throw Exception('Cloud mode is unavailable on this build.');
    }
    return client;
  }
}
