import 'package:beltech/data/local/drift/app_drift_store.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

enum DataStoreMode { local, cloud }

class DataMigrationPreview {
  const DataMigrationPreview({
    required this.fromMode,
    required this.toMode,
    required this.sourceCounts,
    required this.destinationCounts,
    required this.canMigrate,
    this.blockerMessage,
  });

  final DataStoreMode fromMode;
  final DataStoreMode toMode;
  final Map<String, int> sourceCounts;
  final Map<String, int> destinationCounts;
  final bool canMigrate;
  final String? blockerMessage;

  int get sourceTotal =>
      sourceCounts.values.fold<int>(0, (sum, value) => sum + value);
  int get destinationTotal =>
      destinationCounts.values.fold<int>(0, (sum, value) => sum + value);
}

class DataMigrationResult {
  const DataMigrationResult({
    required this.insertedCounts,
    required this.updatedCounts,
  });

  final Map<String, int> insertedCounts;
  final Map<String, int> updatedCounts;

  int get insertedTotal =>
      insertedCounts.values.fold<int>(0, (sum, value) => sum + value);
  int get updatedTotal =>
      updatedCounts.values.fold<int>(0, (sum, value) => sum + value);

  bool get changed => insertedTotal > 0 || updatedTotal > 0;
}

class DataModeMigrationService {
  DataModeMigrationService({
    required AppDriftStore localStore,
    required SupabaseClient? supabaseClient,
  })  : _localStore = localStore,
        _supabaseClient = supabaseClient;

  final AppDriftStore _localStore;
  final SupabaseClient? _supabaseClient;

  static const String scopeExpenses = 'expenses';
  static const String scopeIncome = 'income';
  static const String scopeTasks = 'tasks';
  static const String scopeEvents = 'events';
  static const String scopeBudgets = 'budgets';
  static const String scopeRecurring = 'recurring';

  static const List<String> orderedScopes = [
    scopeExpenses,
    scopeIncome,
    scopeTasks,
    scopeEvents,
    scopeBudgets,
    scopeRecurring,
  ];

  Future<DataMigrationPreview> preview({
    required DataStoreMode fromMode,
    required DataStoreMode toMode,
    required bool cloudAvailable,
  }) async {
    final blocker = _resolveBlocker(
      fromMode: fromMode,
      toMode: toMode,
      cloudAvailable: cloudAvailable,
    );
    final userId = _currentCloudUserId();

    final sourceCounts = await _safeLoadCounts(
      mode: fromMode,
      userId: userId,
      blocked: blocker != null,
    );
    final destinationCounts = await _safeLoadCounts(
      mode: toMode,
      userId: userId,
      blocked: blocker != null,
    );

    return DataMigrationPreview(
      fromMode: fromMode,
      toMode: toMode,
      sourceCounts: sourceCounts,
      destinationCounts: destinationCounts,
      canMigrate: blocker == null,
      blockerMessage: blocker,
    );
  }

  Future<DataMigrationResult> migrate({
    required DataStoreMode fromMode,
    required DataStoreMode toMode,
    required bool cloudAvailable,
  }) async {
    if (fromMode == toMode) {
      return DataMigrationResult(
        insertedCounts: _emptyScopeCounts(),
        updatedCounts: _emptyScopeCounts(),
      );
    }

    final blocker = _resolveBlocker(
      fromMode: fromMode,
      toMode: toMode,
      cloudAvailable: cloudAvailable,
    );
    if (blocker != null) {
      throw Exception(blocker);
    }

    final userId = _requireCloudUserId();
    final payload = fromMode == DataStoreMode.local
        ? await _readLocalPayload()
        : await _readCloudPayload(userId);

    if (toMode == DataStoreMode.local) {
      return _writeLocalPayload(payload);
    }
    return _writeCloudPayload(payload, userId);
  }

  String? _resolveBlocker({
    required DataStoreMode fromMode,
    required DataStoreMode toMode,
    required bool cloudAvailable,
  }) {
    if (fromMode == toMode) {
      return 'Source and destination are the same.';
    }
    if ((fromMode == DataStoreMode.cloud || toMode == DataStoreMode.cloud) &&
        !cloudAvailable) {
      return 'Cloud mode is unavailable on this build.';
    }
    if (fromMode == DataStoreMode.cloud || toMode == DataStoreMode.cloud) {
      if (_supabaseClient == null) {
        return 'Cloud mode is unavailable on this build.';
      }
      if (_currentCloudUserId() == null) {
        return 'Sign in to your cloud account to migrate data.';
      }
    }
    return null;
  }

  Future<Map<String, int>> _safeLoadCounts({
    required DataStoreMode mode,
    required String? userId,
    required bool blocked,
  }) async {
    if (blocked) {
      if (mode == DataStoreMode.cloud) {
        return _emptyScopeCounts();
      }
      return _loadLocalCounts();
    }
    return _loadCounts(mode: mode, userId: userId);
  }

  Future<Map<String, int>> _loadCounts({
    required DataStoreMode mode,
    required String? userId,
  }) {
    if (mode == DataStoreMode.local) {
      return _loadLocalCounts();
    }
    if (userId == null || userId.isEmpty) {
      return Future.value(_emptyScopeCounts());
    }
    return _loadCloudCounts(userId);
  }

  Future<Map<String, int>> _loadLocalCounts() async {
    await _localStore.ensureInitialized();
    final counts = _emptyScopeCounts();
    counts[scopeExpenses] = await _countLocal('transactions');
    counts[scopeIncome] = await _countLocal('incomes');
    counts[scopeTasks] = await _countLocal('tasks');
    counts[scopeEvents] = await _countLocal('events');
    counts[scopeBudgets] = await _countLocal('budgets');
    counts[scopeRecurring] = await _countLocal('recurring_templates');
    return counts;
  }

  Future<Map<String, int>> _loadCloudCounts(String userId) async {
    final client = _requireClient();
    final counts = _emptyScopeCounts();

    final transactions =
        await client.from('transactions').select('id').eq('owner_id', userId);
    counts[scopeExpenses] = (transactions as List).length;

    final incomes =
        await client.from('incomes').select('id').eq('owner_id', userId);
    counts[scopeIncome] = (incomes as List).length;

    final tasks =
        await client.from('tasks').select('id').eq('owner_id', userId);
    counts[scopeTasks] = (tasks as List).length;

    final events =
        await client.from('events').select('id').eq('owner_id', userId);
    counts[scopeEvents] = (events as List).length;

    final budgets =
        await client.from('budgets').select('id').eq('owner_id', userId);
    counts[scopeBudgets] = (budgets as List).length;

    final recurring = await client
        .from('recurring_templates')
        .select('id')
        .eq('owner_id', userId);
    counts[scopeRecurring] = (recurring as List).length;

    return counts;
  }

  Future<int> _countLocal(String table) async {
    final rows = await _localStore.executor
        .runSelect('SELECT COUNT(*) AS total FROM $table', const []);
    final row = rows.isEmpty ? const <String, Object?>{} : rows.first;
    return _asInt(row['total']);
  }

  Future<_MigrationPayload> _readLocalPayload() async {
    await _localStore.ensureInitialized();
    final transactions = _toMapList(await _localStore.executor.runSelect(
      'SELECT title, category, amount, occurred_at, source, source_hash FROM transactions',
      const [],
    ));
    final incomes = _toMapList(await _localStore.executor.runSelect(
      'SELECT title, amount, received_at, source FROM incomes',
      const [],
    ));
    final tasks = _toMapList(await _localStore.executor.runSelect(
      'SELECT title, description, completed, due_at, priority FROM tasks',
      const [],
    ));
    final events = _toMapList(await _localStore.executor.runSelect(
      'SELECT title, start_at, end_at, note, completed, priority, event_type FROM events',
      const [],
    ));
    final budgets = _toMapList(await _localStore.executor.runSelect(
      'SELECT category, monthly_limit FROM budgets',
      const [],
    ));
    final recurring = _toMapList(await _localStore.executor.runSelect(
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

  Future<_MigrationPayload> _readCloudPayload(String userId) async {
    final client = _requireClient();
    final transactions = _toMapList(await client
        .from('transactions')
        .select('title,category,amount,occurred_at,source,source_hash')
        .eq('owner_id', userId));
    final incomes = _toMapList(await client
        .from('incomes')
        .select('title,amount,received_at,source')
        .eq('owner_id', userId));
    final tasks = _toMapList(await client
        .from('tasks')
        .select('title,description,completed,due_at,priority')
        .eq('owner_id', userId));
    final events = _toMapList(await client
        .from('events')
        .select('title,start_at,end_at,note,completed,priority,event_type')
        .eq('owner_id', userId));
    final budgets = _toMapList(await client
        .from('budgets')
        .select('category,monthly_limit')
        .eq('owner_id', userId));
    final recurring = _toMapList(await client
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

  Future<DataMigrationResult> _writeLocalPayload(
      _MigrationPayload payload) async {
    await _localStore.ensureInitialized();

    final inserted = _emptyScopeCounts();
    final updated = _emptyScopeCounts();
    var changed = false;

    final existingTransactions =
        _toMapList(await _localStore.executor.runSelect(
      'SELECT title, category, amount, occurred_at, source, source_hash FROM transactions',
      const [],
    ));
    final transactionSignatures =
        existingTransactions.map(_transactionSignature).toSet();
    for (final row in payload.transactions) {
      final signature = _transactionSignature(row);
      if (!transactionSignatures.add(signature)) {
        continue;
      }
      await _localStore.executor.runInsert(
        'INSERT INTO transactions(title, category, amount, occurred_at, source, source_hash) VALUES (?, ?, ?, ?, ?, ?)',
        [
          _requiredText(row['title'], fallback: 'Untitled'),
          _requiredText(row['category'], fallback: 'Other'),
          _asDouble(row['amount']),
          _asEpochMs(row['occurred_at']),
          _requiredText(row['source'], fallback: 'manual'),
          _nullableText(row['source_hash']),
        ],
      );
      inserted[scopeExpenses] = (inserted[scopeExpenses] ?? 0) + 1;
      changed = true;
    }

    final existingIncomes = _toMapList(await _localStore.executor.runSelect(
      'SELECT title, amount, received_at, source FROM incomes',
      const [],
    ));
    final incomeSignatures = existingIncomes.map(_incomeSignature).toSet();
    for (final row in payload.incomes) {
      final signature = _incomeSignature(row);
      if (!incomeSignatures.add(signature)) {
        continue;
      }
      await _localStore.executor.runInsert(
        'INSERT INTO incomes(title, amount, received_at, source) VALUES (?, ?, ?, ?)',
        [
          _requiredText(row['title'], fallback: 'Income'),
          _asDouble(row['amount']),
          _asEpochMs(row['received_at']),
          _requiredText(row['source'], fallback: 'manual'),
        ],
      );
      inserted[scopeIncome] = (inserted[scopeIncome] ?? 0) + 1;
      changed = true;
    }

    final existingTasks = _toMapList(await _localStore.executor.runSelect(
      'SELECT title, description, completed, due_at, priority FROM tasks',
      const [],
    ));
    final taskSignatures = existingTasks.map(_taskSignature).toSet();
    for (final row in payload.tasks) {
      final signature = _taskSignature(row);
      if (!taskSignatures.add(signature)) {
        continue;
      }
      await _localStore.executor.runInsert(
        'INSERT INTO tasks(title, description, completed, due_at, priority) VALUES (?, ?, ?, ?, ?)',
        [
          _requiredText(row['title'], fallback: 'Task'),
          _nullableText(row['description']),
          _asBool(row['completed']) ? 1 : 0,
          _nullableEpochMs(row['due_at']),
          _requiredText(row['priority'], fallback: 'medium'),
        ],
      );
      inserted[scopeTasks] = (inserted[scopeTasks] ?? 0) + 1;
      changed = true;
    }

    final existingEvents = _toMapList(await _localStore.executor.runSelect(
      'SELECT title, start_at, end_at, note, completed, priority, event_type FROM events',
      const [],
    ));
    final eventSignatures = existingEvents.map(_eventSignature).toSet();
    for (final row in payload.events) {
      final signature = _eventSignature(row);
      if (!eventSignatures.add(signature)) {
        continue;
      }
      await _localStore.executor.runInsert(
        'INSERT INTO events(title, start_at, end_at, note, completed, priority, event_type) VALUES (?, ?, ?, ?, ?, ?, ?)',
        [
          _requiredText(row['title'], fallback: 'Event'),
          _asEpochMs(row['start_at']),
          _nullableEpochMs(row['end_at']),
          _nullableText(row['note']),
          _asBool(row['completed']) ? 1 : 0,
          _requiredText(row['priority'], fallback: 'medium'),
          _requiredText(row['event_type'], fallback: 'general'),
        ],
      );
      inserted[scopeEvents] = (inserted[scopeEvents] ?? 0) + 1;
      changed = true;
    }

    final existingBudgets = _toMapList(await _localStore.executor.runSelect(
      'SELECT id, category, monthly_limit FROM budgets',
      const [],
    ));
    final budgetByCategory = <String, Map<String, dynamic>>{};
    for (final row in existingBudgets) {
      budgetByCategory[_normalized(row['category'])] = row;
    }
    for (final row in payload.budgets) {
      final category = _requiredText(row['category'], fallback: '');
      if (category.isEmpty) {
        continue;
      }
      final key = _normalized(category);
      final nextLimit = _asDouble(row['monthly_limit']);
      final existing = budgetByCategory[key];
      if (existing == null) {
        await _localStore.executor.runInsert(
          'INSERT INTO budgets(category, monthly_limit) VALUES (?, ?)',
          [category, nextLimit],
        );
        budgetByCategory[key] = {
          'category': category,
          'monthly_limit': nextLimit,
        };
        inserted[scopeBudgets] = (inserted[scopeBudgets] ?? 0) + 1;
        changed = true;
        continue;
      }
      final currentLimit = _asDouble(existing['monthly_limit']);
      final currentCategory = _requiredText(existing['category'], fallback: '');
      if (_doubleEquals(currentLimit, nextLimit) &&
          currentCategory == category) {
        continue;
      }
      await _localStore.executor.runUpdate(
        'UPDATE budgets SET category = ?, monthly_limit = ? WHERE id = ?',
        [category, nextLimit, _asInt(existing['id'])],
      );
      existing['category'] = category;
      existing['monthly_limit'] = nextLimit;
      updated[scopeBudgets] = (updated[scopeBudgets] ?? 0) + 1;
      changed = true;
    }

    final existingRecurring = _toMapList(await _localStore.executor.runSelect(
      'SELECT kind, title, description, category, amount, priority, cadence, next_run_at, enabled FROM recurring_templates',
      const [],
    ));
    final recurringSignatures =
        existingRecurring.map(_recurringSignature).toSet();
    for (final row in payload.recurring) {
      final signature = _recurringSignature(row);
      if (!recurringSignatures.add(signature)) {
        continue;
      }
      await _localStore.executor.runInsert(
        'INSERT INTO recurring_templates(kind, title, description, category, amount, priority, cadence, next_run_at, enabled) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)',
        [
          _requiredText(row['kind'], fallback: 'expense'),
          _requiredText(row['title'], fallback: 'Template'),
          _nullableText(row['description']),
          _nullableText(row['category']),
          _nullableDouble(row['amount']),
          _nullableText(row['priority']),
          _requiredText(row['cadence'], fallback: 'daily'),
          _asEpochMs(row['next_run_at']),
          _asBool(row['enabled']) ? 1 : 0,
        ],
      );
      inserted[scopeRecurring] = (inserted[scopeRecurring] ?? 0) + 1;
      changed = true;
    }

    if (changed) {
      _localStore.emitChange();
    }

    return DataMigrationResult(
      insertedCounts: inserted,
      updatedCounts: updated,
    );
  }

  Future<DataMigrationResult> _writeCloudPayload(
    _MigrationPayload payload,
    String userId,
  ) async {
    final client = _requireClient();
    final inserted = _emptyScopeCounts();
    final updated = _emptyScopeCounts();

    final existingTransactions = _toMapList(await client
        .from('transactions')
        .select('title,category,amount,occurred_at,source,source_hash')
        .eq('owner_id', userId));
    final transactionSignatures =
        existingTransactions.map(_transactionSignature).toSet();
    final transactionInsertPayload = <Map<String, dynamic>>[];
    for (final row in payload.transactions) {
      final signature = _transactionSignature(row);
      if (!transactionSignatures.add(signature)) {
        continue;
      }
      transactionInsertPayload.add({
        'owner_id': userId,
        'title': _requiredText(row['title'], fallback: 'Untitled'),
        'category': _requiredText(row['category'], fallback: 'Other'),
        'amount': _asDouble(row['amount']),
        'occurred_at': _requiredIsoUtc(row['occurred_at']),
        'source': _requiredText(row['source'], fallback: 'manual'),
        'source_hash': _nullableText(row['source_hash']),
      });
    }
    if (transactionInsertPayload.isNotEmpty) {
      await client.from('transactions').insert(transactionInsertPayload);
      inserted[scopeExpenses] = transactionInsertPayload.length;
    }

    final existingIncomes = _toMapList(await client
        .from('incomes')
        .select('title,amount,received_at,source')
        .eq('owner_id', userId));
    final incomeSignatures = existingIncomes.map(_incomeSignature).toSet();
    final incomeInsertPayload = <Map<String, dynamic>>[];
    for (final row in payload.incomes) {
      final signature = _incomeSignature(row);
      if (!incomeSignatures.add(signature)) {
        continue;
      }
      incomeInsertPayload.add({
        'owner_id': userId,
        'title': _requiredText(row['title'], fallback: 'Income'),
        'amount': _asDouble(row['amount']),
        'received_at': _requiredIsoUtc(row['received_at']),
        'source': _requiredText(row['source'], fallback: 'manual'),
      });
    }
    if (incomeInsertPayload.isNotEmpty) {
      await client.from('incomes').insert(incomeInsertPayload);
      inserted[scopeIncome] = incomeInsertPayload.length;
    }

    final existingTasks = _toMapList(await client
        .from('tasks')
        .select('title,description,completed,due_at,priority')
        .eq('owner_id', userId));
    final taskSignatures = existingTasks.map(_taskSignature).toSet();
    final taskInsertPayload = <Map<String, dynamic>>[];
    for (final row in payload.tasks) {
      final signature = _taskSignature(row);
      if (!taskSignatures.add(signature)) {
        continue;
      }
      taskInsertPayload.add({
        'owner_id': userId,
        'title': _requiredText(row['title'], fallback: 'Task'),
        'description': _nullableText(row['description']),
        'completed': _asBool(row['completed']),
        'due_at': _nullableIsoUtc(row['due_at']),
        'priority': _requiredText(row['priority'], fallback: 'medium'),
      });
    }
    if (taskInsertPayload.isNotEmpty) {
      await client.from('tasks').insert(taskInsertPayload);
      inserted[scopeTasks] = taskInsertPayload.length;
    }

    final existingEvents = _toMapList(await client
        .from('events')
        .select('title,start_at,end_at,note,completed,priority,event_type')
        .eq('owner_id', userId));
    final eventSignatures = existingEvents.map(_eventSignature).toSet();
    final eventInsertPayload = <Map<String, dynamic>>[];
    for (final row in payload.events) {
      final signature = _eventSignature(row);
      if (!eventSignatures.add(signature)) {
        continue;
      }
      eventInsertPayload.add({
        'owner_id': userId,
        'title': _requiredText(row['title'], fallback: 'Event'),
        'start_at': _requiredIsoUtc(row['start_at']),
        'end_at': _nullableIsoUtc(row['end_at']),
        'note': _nullableText(row['note']),
        'completed': _asBool(row['completed']),
        'priority': _requiredText(row['priority'], fallback: 'medium'),
        'event_type': _requiredText(row['event_type'], fallback: 'general'),
      });
    }
    if (eventInsertPayload.isNotEmpty) {
      await client.from('events').insert(eventInsertPayload);
      inserted[scopeEvents] = eventInsertPayload.length;
    }

    final existingBudgets = _toMapList(await client
        .from('budgets')
        .select('id,category,monthly_limit')
        .eq('owner_id', userId));
    final budgetByCategory = <String, Map<String, dynamic>>{};
    for (final row in existingBudgets) {
      budgetByCategory[_normalized(row['category'])] = row;
    }
    final budgetInsertPayload = <Map<String, dynamic>>[];
    for (final row in payload.budgets) {
      final category = _requiredText(row['category'], fallback: '');
      if (category.isEmpty) {
        continue;
      }
      final key = _normalized(category);
      final nextLimit = _asDouble(row['monthly_limit']);
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
      final currentLimit = _asDouble(existing['monthly_limit']);
      final currentCategory = _requiredText(existing['category'], fallback: '');
      if (_doubleEquals(currentLimit, nextLimit) &&
          currentCategory == category) {
        continue;
      }
      await client
          .from('budgets')
          .update({'category': category, 'monthly_limit': nextLimit})
          .eq('owner_id', userId)
          .eq('id', _asInt(existing['id']));
      existing['category'] = category;
      existing['monthly_limit'] = nextLimit;
      updated[scopeBudgets] = (updated[scopeBudgets] ?? 0) + 1;
    }
    if (budgetInsertPayload.isNotEmpty) {
      await client.from('budgets').insert(budgetInsertPayload);
      inserted[scopeBudgets] = budgetInsertPayload.length;
    }

    final existingRecurring = _toMapList(await client
        .from('recurring_templates')
        .select(
            'kind,title,description,category,amount,priority,cadence,next_run_at,enabled')
        .eq('owner_id', userId));
    final recurringSignatures =
        existingRecurring.map(_recurringSignature).toSet();
    final recurringInsertPayload = <Map<String, dynamic>>[];
    for (final row in payload.recurring) {
      final signature = _recurringSignature(row);
      if (!recurringSignatures.add(signature)) {
        continue;
      }
      recurringInsertPayload.add({
        'owner_id': userId,
        'kind': _requiredText(row['kind'], fallback: 'expense'),
        'title': _requiredText(row['title'], fallback: 'Template'),
        'description': _nullableText(row['description']),
        'category': _nullableText(row['category']),
        'amount': _nullableDouble(row['amount']),
        'priority': _nullableText(row['priority']),
        'cadence': _requiredText(row['cadence'], fallback: 'daily'),
        'next_run_at': _requiredIsoUtc(row['next_run_at']),
        'enabled': _asBool(row['enabled']),
      });
    }
    if (recurringInsertPayload.isNotEmpty) {
      await client.from('recurring_templates').insert(recurringInsertPayload);
      inserted[scopeRecurring] = recurringInsertPayload.length;
    }

    return DataMigrationResult(
      insertedCounts: inserted,
      updatedCounts: updated,
    );
  }

  Map<String, int> _emptyScopeCounts() {
    return {
      for (final scope in orderedScopes) scope: 0,
    };
  }

  List<Map<String, dynamic>> _toMapList(dynamic rows) {
    if (rows is! List) {
      return const [];
    }
    final normalized = <Map<String, dynamic>>[];
    for (final row in rows) {
      if (row is Map<String, dynamic>) {
        normalized.add(Map<String, dynamic>.from(row));
        continue;
      }
      if (row is Map) {
        normalized.add(
          row.map((key, value) => MapEntry('$key', value)),
        );
      }
    }
    return normalized;
  }

  SupabaseClient _requireClient() {
    final client = _supabaseClient;
    if (client == null) {
      throw Exception('Cloud mode is unavailable on this build.');
    }
    return client;
  }

  String? _currentCloudUserId() {
    return _supabaseClient?.auth.currentUser?.id;
  }

  String _requireCloudUserId() {
    final userId = _currentCloudUserId();
    if (userId == null || userId.isEmpty) {
      throw Exception('Sign in to your cloud account to migrate data.');
    }
    return userId;
  }

  String _transactionSignature(Map<String, dynamic> row) {
    final sourceHash = _nullableText(row['source_hash']);
    if (sourceHash != null && sourceHash.isNotEmpty) {
      return 'hash:${_normalized(sourceHash)}';
    }
    return [
      _normalized(row['title']),
      _normalized(row['category']),
      _doubleSignature(row['amount']),
      _asEpochMs(row['occurred_at']),
      _normalized(row['source']),
    ].join('|');
  }

  String _incomeSignature(Map<String, dynamic> row) {
    return [
      _normalized(row['title']),
      _doubleSignature(row['amount']),
      _asEpochMs(row['received_at']),
      _normalized(row['source']),
    ].join('|');
  }

  String _taskSignature(Map<String, dynamic> row) {
    return [
      _normalized(row['title']),
      _normalized(row['description']),
      _asBool(row['completed']) ? '1' : '0',
      _nullableEpochMs(row['due_at']) ?? 0,
      _normalized(row['priority']),
    ].join('|');
  }

  String _eventSignature(Map<String, dynamic> row) {
    return [
      _normalized(row['title']),
      _asEpochMs(row['start_at']),
      _nullableEpochMs(row['end_at']) ?? 0,
      _normalized(row['note']),
      _asBool(row['completed']) ? '1' : '0',
      _normalized(row['priority']),
      _normalized(row['event_type']),
    ].join('|');
  }

  String _recurringSignature(Map<String, dynamic> row) {
    return [
      _normalized(row['kind']),
      _normalized(row['title']),
      _normalized(row['description']),
      _normalized(row['category']),
      _optionalDoubleSignature(row['amount']),
      _normalized(row['priority']),
      _normalized(row['cadence']),
      _asEpochMs(row['next_run_at']),
      _asBool(row['enabled']) ? '1' : '0',
    ].join('|');
  }

  int _asInt(Object? value) {
    if (value is int) {
      return value;
    }
    if (value is num) {
      return value.toInt();
    }
    return int.tryParse('${value ?? ''}') ?? 0;
  }

  double _asDouble(Object? value) {
    if (value is double) {
      return value;
    }
    if (value is num) {
      return value.toDouble();
    }
    return double.tryParse('${value ?? ''}') ?? 0;
  }

  double? _nullableDouble(Object? value) {
    if (value == null) {
      return null;
    }
    final text = '$value'.trim();
    if (text.isEmpty) {
      return null;
    }
    if (value is num) {
      return value.toDouble();
    }
    return double.tryParse(text);
  }

  bool _asBool(Object? value) {
    if (value is bool) {
      return value;
    }
    if (value is num) {
      return value != 0;
    }
    final normalized = '${value ?? ''}'.trim().toLowerCase();
    return normalized == 'true' || normalized == '1' || normalized == 'yes';
  }

  int _asEpochMs(Object? value) {
    if (value == null) {
      return 0;
    }
    if (value is DateTime) {
      return value.toUtc().millisecondsSinceEpoch;
    }
    if (value is int) {
      return value;
    }
    if (value is num) {
      return value.toInt();
    }
    final text = '$value'.trim();
    if (text.isEmpty) {
      return 0;
    }
    final parsedTime = DateTime.tryParse(text);
    if (parsedTime != null) {
      return parsedTime.toUtc().millisecondsSinceEpoch;
    }
    return int.tryParse(text) ?? 0;
  }

  int? _nullableEpochMs(Object? value) {
    if (value == null) {
      return null;
    }
    final text = '$value'.trim();
    if (text.isEmpty) {
      return null;
    }
    return _asEpochMs(value);
  }

  String _requiredIsoUtc(Object? value) {
    final ms = _asEpochMs(value);
    final safeMs = ms > 0 ? ms : DateTime.now().toUtc().millisecondsSinceEpoch;
    return DateTime.fromMillisecondsSinceEpoch(safeMs, isUtc: true)
        .toIso8601String();
  }

  String? _nullableIsoUtc(Object? value) {
    final ms = _nullableEpochMs(value);
    if (ms == null) {
      return null;
    }
    return DateTime.fromMillisecondsSinceEpoch(ms, isUtc: true)
        .toIso8601String();
  }

  String _requiredText(Object? value, {required String fallback}) {
    final text = '${value ?? ''}'.trim();
    return text.isEmpty ? fallback : text;
  }

  String? _nullableText(Object? value) {
    if (value == null) {
      return null;
    }
    final text = '$value'.trim();
    return text.isEmpty ? null : text;
  }

  String _normalized(Object? value) {
    return '${value ?? ''}'.trim().toLowerCase();
  }

  String _doubleSignature(Object? value) {
    return _asDouble(value).toStringAsFixed(4);
  }

  String _optionalDoubleSignature(Object? value) {
    final normalized = '${value ?? ''}'.trim();
    if (normalized.isEmpty) {
      return '';
    }
    return _asDouble(value).toStringAsFixed(4);
  }

  bool _doubleEquals(double left, double right) {
    return (left - right).abs() < 0.0001;
  }
}

class _MigrationPayload {
  const _MigrationPayload({
    required this.transactions,
    required this.incomes,
    required this.tasks,
    required this.events,
    required this.budgets,
    required this.recurring,
  });

  final List<Map<String, dynamic>> transactions;
  final List<Map<String, dynamic>> incomes;
  final List<Map<String, dynamic>> tasks;
  final List<Map<String, dynamic>> events;
  final List<Map<String, dynamic>> budgets;
  final List<Map<String, dynamic>> recurring;
}
