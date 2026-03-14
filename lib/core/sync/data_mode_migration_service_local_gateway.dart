part of 'data_mode_migration_service.dart';

class _LocalMigrationGateway {
  _LocalMigrationGateway({
    required this.store,
    required this.codec,
    required this.orderedScopes,
  });

  final AppDriftStore store;
  final _MigrationCodec codec;
  final List<String> orderedScopes;

  Future<Map<String, int>> loadCounts() async {
    await store.ensureInitialized();
    final counts = codec.emptyScopeCounts(orderedScopes);
    counts[DataModeMigrationService.scopeExpenses] =
        await _countLocal('transactions');
    counts[DataModeMigrationService.scopeIncome] = await _countLocal('incomes');
    counts[DataModeMigrationService.scopeTasks] = await _countLocal('tasks');
    counts[DataModeMigrationService.scopeEvents] = await _countLocal('events');
    counts[DataModeMigrationService.scopeBudgets] = await _countLocal('budgets');
    counts[DataModeMigrationService.scopeRecurring] =
        await _countLocal('recurring_templates');
    return counts;
  }

  Future<_MigrationPayload> readPayload() async {
    await store.ensureInitialized();
    final transactions = codec.toMapList(await store.executor.runSelect(
      'SELECT title, category, amount, occurred_at, source, source_hash FROM transactions',
      const [],
    ));
    final incomes = codec.toMapList(await store.executor.runSelect(
      'SELECT title, amount, received_at, source FROM incomes',
      const [],
    ));
    final tasks = codec.toMapList(await store.executor.runSelect(
      'SELECT title, description, completed, due_at, priority FROM tasks',
      const [],
    ));
    final events = codec.toMapList(await store.executor.runSelect(
      'SELECT title, start_at, end_at, note, completed, priority, event_type FROM events',
      const [],
    ));
    final budgets = codec.toMapList(await store.executor.runSelect(
      'SELECT category, monthly_limit FROM budgets',
      const [],
    ));
    final recurring = codec.toMapList(await store.executor.runSelect(
      'SELECT kind, title, description, category, amount, priority, cadence, next_run_at, enabled FROM recurring_templates',
      const [],
    ));

    return _MigrationPayload(
      transactions: transactions,
      incomes: incomes,
      tasks: tasks,
      events: events,
      budgets: budgets,
      recurring: recurring,
    );
  }

  Future<DataMigrationResult> writePayload(_MigrationPayload payload) async {
    await store.ensureInitialized();

    final inserted = codec.emptyScopeCounts(orderedScopes);
    final updated = codec.emptyScopeCounts(orderedScopes);
    var changed = false;

    final existingTransactions = codec.toMapList(await store.executor.runSelect(
      'SELECT title, category, amount, occurred_at, source, source_hash FROM transactions',
      const [],
    ));
    final transactionSignatures =
        existingTransactions.map(codec.transactionSignature).toSet();
    for (final row in payload.transactions) {
      final signature = codec.transactionSignature(row);
      if (!transactionSignatures.add(signature)) {
        continue;
      }
      await store.executor.runInsert(
        'INSERT INTO transactions(title, category, amount, occurred_at, source, source_hash) VALUES (?, ?, ?, ?, ?, ?)',
        [
          codec.requiredText(row['title'], fallback: 'Untitled'),
          codec.requiredText(row['category'], fallback: 'Other'),
          codec.asDouble(row['amount']),
          codec.asEpochMs(row['occurred_at']),
          codec.requiredText(row['source'], fallback: 'manual'),
          codec.nullableText(row['source_hash']),
        ],
      );
      inserted[DataModeMigrationService.scopeExpenses] =
          (inserted[DataModeMigrationService.scopeExpenses] ?? 0) + 1;
      changed = true;
    }

    final existingIncomes = codec.toMapList(await store.executor.runSelect(
      'SELECT title, amount, received_at, source FROM incomes',
      const [],
    ));
    final incomeSignatures = existingIncomes.map(codec.incomeSignature).toSet();
    for (final row in payload.incomes) {
      final signature = codec.incomeSignature(row);
      if (!incomeSignatures.add(signature)) {
        continue;
      }
      await store.executor.runInsert(
        'INSERT INTO incomes(title, amount, received_at, source) VALUES (?, ?, ?, ?)',
        [
          codec.requiredText(row['title'], fallback: 'Income'),
          codec.asDouble(row['amount']),
          codec.asEpochMs(row['received_at']),
          codec.requiredText(row['source'], fallback: 'manual'),
        ],
      );
      inserted[DataModeMigrationService.scopeIncome] =
          (inserted[DataModeMigrationService.scopeIncome] ?? 0) + 1;
      changed = true;
    }

    final existingTasks = codec.toMapList(await store.executor.runSelect(
      'SELECT title, description, completed, due_at, priority FROM tasks',
      const [],
    ));
    final taskSignatures = existingTasks.map(codec.taskSignature).toSet();
    for (final row in payload.tasks) {
      final signature = codec.taskSignature(row);
      if (!taskSignatures.add(signature)) {
        continue;
      }
      await store.executor.runInsert(
        'INSERT INTO tasks(title, description, completed, due_at, priority) VALUES (?, ?, ?, ?, ?)',
        [
          codec.requiredText(row['title'], fallback: 'Task'),
          codec.nullableText(row['description']),
          codec.asBool(row['completed']) ? 1 : 0,
          codec.nullableEpochMs(row['due_at']),
          codec.requiredText(row['priority'], fallback: 'medium'),
        ],
      );
      inserted[DataModeMigrationService.scopeTasks] =
          (inserted[DataModeMigrationService.scopeTasks] ?? 0) + 1;
      changed = true;
    }

    final existingEvents = codec.toMapList(await store.executor.runSelect(
      'SELECT title, start_at, end_at, note, completed, priority, event_type FROM events',
      const [],
    ));
    final eventSignatures = existingEvents.map(codec.eventSignature).toSet();
    for (final row in payload.events) {
      final signature = codec.eventSignature(row);
      if (!eventSignatures.add(signature)) {
        continue;
      }
      await store.executor.runInsert(
        'INSERT INTO events(title, start_at, end_at, note, completed, priority, event_type) VALUES (?, ?, ?, ?, ?, ?, ?)',
        [
          codec.requiredText(row['title'], fallback: 'Event'),
          codec.asEpochMs(row['start_at']),
          codec.nullableEpochMs(row['end_at']),
          codec.nullableText(row['note']),
          codec.asBool(row['completed']) ? 1 : 0,
          codec.requiredText(row['priority'], fallback: 'medium'),
          codec.requiredText(row['event_type'], fallback: 'general'),
        ],
      );
      inserted[DataModeMigrationService.scopeEvents] =
          (inserted[DataModeMigrationService.scopeEvents] ?? 0) + 1;
      changed = true;
    }

    final existingBudgets = codec.toMapList(await store.executor.runSelect(
      'SELECT id, category, monthly_limit FROM budgets',
      const [],
    ));
    final budgetByCategory = <String, Map<String, dynamic>>{};
    for (final row in existingBudgets) {
      budgetByCategory[codec.normalized(row['category'])] = row;
    }
    for (final row in payload.budgets) {
      final category = codec.requiredText(row['category'], fallback: '');
      if (category.isEmpty) {
        continue;
      }
      final key = codec.normalized(category);
      final nextLimit = codec.asDouble(row['monthly_limit']);
      final existing = budgetByCategory[key];
      if (existing == null) {
        await store.executor.runInsert(
          'INSERT INTO budgets(category, monthly_limit) VALUES (?, ?)',
          [category, nextLimit],
        );
        budgetByCategory[key] = {
          'category': category,
          'monthly_limit': nextLimit,
        };
        inserted[DataModeMigrationService.scopeBudgets] =
            (inserted[DataModeMigrationService.scopeBudgets] ?? 0) + 1;
        changed = true;
        continue;
      }
      final currentLimit = codec.asDouble(existing['monthly_limit']);
      final currentCategory = codec.requiredText(existing['category'], fallback: '');
      if (codec.doubleEquals(currentLimit, nextLimit) &&
          currentCategory == category) {
        continue;
      }
      await store.executor.runUpdate(
        'UPDATE budgets SET category = ?, monthly_limit = ? WHERE id = ?',
        [category, nextLimit, codec.asInt(existing['id'])],
      );
      existing['category'] = category;
      existing['monthly_limit'] = nextLimit;
      updated[DataModeMigrationService.scopeBudgets] =
          (updated[DataModeMigrationService.scopeBudgets] ?? 0) + 1;
      changed = true;
    }

    final existingRecurring = codec.toMapList(await store.executor.runSelect(
      'SELECT kind, title, description, category, amount, priority, cadence, next_run_at, enabled FROM recurring_templates',
      const [],
    ));
    final recurringSignatures =
        existingRecurring.map(codec.recurringSignature).toSet();
    for (final row in payload.recurring) {
      final signature = codec.recurringSignature(row);
      if (!recurringSignatures.add(signature)) {
        continue;
      }
      await store.executor.runInsert(
        'INSERT INTO recurring_templates(kind, title, description, category, amount, priority, cadence, next_run_at, enabled) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)',
        [
          codec.requiredText(row['kind'], fallback: 'expense'),
          codec.requiredText(row['title'], fallback: 'Template'),
          codec.nullableText(row['description']),
          codec.nullableText(row['category']),
          codec.nullableDouble(row['amount']),
          codec.nullableText(row['priority']),
          codec.requiredText(row['cadence'], fallback: 'daily'),
          codec.asEpochMs(row['next_run_at']),
          codec.asBool(row['enabled']) ? 1 : 0,
        ],
      );
      inserted[DataModeMigrationService.scopeRecurring] =
          (inserted[DataModeMigrationService.scopeRecurring] ?? 0) + 1;
      changed = true;
    }

    if (changed) {
      store.emitChange();
    }

    return DataMigrationResult(insertedCounts: inserted, updatedCounts: updated);
  }

  Future<int> _countLocal(String table) async {
    final rows =
        await store.executor.runSelect('SELECT COUNT(*) AS total FROM $table', const []);
    final row = rows.isEmpty ? const <String, Object?>{} : rows.first;
    return codec.asInt(row['total']);
  }
}
