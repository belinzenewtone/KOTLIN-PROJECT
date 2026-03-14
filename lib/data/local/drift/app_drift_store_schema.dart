part of 'app_drift_store.dart';

class _AppDriftSchema {
  static Future<void> ensureInitialized(AppDriftStore store) async {
    if (store._initialized) {
      return;
    }
    await store._db.ensureOpen(const _StoreQueryExecutorUser());

    await store._db.runCustom(
      'CREATE TABLE IF NOT EXISTS transactions('
      'id INTEGER PRIMARY KEY AUTOINCREMENT,'
      'title TEXT NOT NULL,'
      'category TEXT NOT NULL,'
      'amount REAL NOT NULL,'
      'occurred_at INTEGER NOT NULL,'
      'source TEXT NOT NULL,'
      'source_hash TEXT'
      ')',
    );
    await tryAddSourceHashColumn(store);

    await store._db.runCustom(
      'CREATE TABLE IF NOT EXISTS tasks('
      'id INTEGER PRIMARY KEY AUTOINCREMENT,'
      'title TEXT NOT NULL,'
      'description TEXT,'
      'completed INTEGER NOT NULL DEFAULT 0,'
      'due_at INTEGER,'
      'priority TEXT NOT NULL DEFAULT \'medium\''
      ')',
    );
    await tryAddTaskDescriptionColumn(store);
    await store._db.runCustom(
      'CREATE TABLE IF NOT EXISTS events('
      'id INTEGER PRIMARY KEY AUTOINCREMENT,'
      'title TEXT NOT NULL,'
      'start_at INTEGER NOT NULL,'
      'end_at INTEGER,'
      'note TEXT,'
      'completed INTEGER NOT NULL DEFAULT 0,'
      'priority TEXT NOT NULL DEFAULT \'medium\','
      'event_type TEXT NOT NULL DEFAULT \'general\''
      ')',
    );
    await tryAddEventCompletedColumn(store);
    await tryAddEventPriorityColumn(store);
    await tryAddEventTypeColumn(store);
    await store._db.runCustom(
      'CREATE TABLE IF NOT EXISTS incomes('
      'id INTEGER PRIMARY KEY AUTOINCREMENT,'
      'title TEXT NOT NULL,'
      'amount REAL NOT NULL,'
      'received_at INTEGER NOT NULL,'
      'source TEXT NOT NULL DEFAULT \'manual\''
      ')',
    );
    await store._db.runCustom(
      'CREATE TABLE IF NOT EXISTS budgets('
      'id INTEGER PRIMARY KEY AUTOINCREMENT,'
      'category TEXT NOT NULL,'
      'monthly_limit REAL NOT NULL'
      ')',
    );
    await store._db.runCustom(
      'CREATE TABLE IF NOT EXISTS recurring_templates('
      'id INTEGER PRIMARY KEY AUTOINCREMENT,'
      'kind TEXT NOT NULL,'
      'title TEXT NOT NULL,'
      'description TEXT,'
      'category TEXT,'
      'amount REAL,'
      'priority TEXT,'
      'cadence TEXT NOT NULL,'
      'next_run_at INTEGER NOT NULL,'
      'enabled INTEGER NOT NULL DEFAULT 1'
      ')',
    );
    await tryAddIncomesSourceColumn(store);
    await tryAddRecurringPriorityColumn(store);

    await store._db.runCustom(
      'CREATE INDEX IF NOT EXISTS idx_tx_occurred_at ON transactions(occurred_at)',
    );
    await store._db.runCustom(
      'CREATE INDEX IF NOT EXISTS idx_tx_category ON transactions(category)',
    );
    await store._db.runCustom(
      'CREATE UNIQUE INDEX IF NOT EXISTS idx_tx_source_hash ON transactions(source_hash)',
    );
    await store._db.runCustom(
      'CREATE INDEX IF NOT EXISTS idx_tasks_completed ON tasks(completed)',
    );
    await store._db.runCustom(
      'CREATE INDEX IF NOT EXISTS idx_events_start_at ON events(start_at)',
    );
    await store._db.runCustom(
      'CREATE INDEX IF NOT EXISTS idx_incomes_received_at ON incomes(received_at)',
    );
    await store._db.runCustom(
      'CREATE UNIQUE INDEX IF NOT EXISTS idx_budgets_category ON budgets(LOWER(category))',
    );
    await store._db.runCustom(
      'CREATE INDEX IF NOT EXISTS idx_recurring_next_run_at ON recurring_templates(next_run_at)',
    );

    await seedDataIfEmpty(store);
    store._initialized = true;
  }

  static Future<void> seedDataIfEmpty(AppDriftStore store) async {
    final txCount = await store._countRows('transactions');
    if (txCount == 0) {
      final now = DateTime.now();
      final entries = [
        (
          'HOTEL DELITOS Via Kopo Kopo',
          'Food',
          140.0,
          now.subtract(const Duration(days: 1))
        ),
        ('GRACE NGULI', 'Other', 100.0, now.subtract(const Duration(days: 2))),
        ('DELITOS HOTEL', 'Food', 400.0, now.subtract(const Duration(days: 3))),
        ('Unknown', 'Other', 623.53, now.subtract(const Duration(days: 4))),
        ('Unknown', 'Other', 865.93, now.subtract(const Duration(days: 5))),
        (
          'Airtime Topup',
          'Airtime',
          50.0,
          now.subtract(const Duration(days: 2))
        ),
        (
          'Electricity Token',
          'Bills',
          20.0,
          now.subtract(const Duration(days: 3))
        ),
      ];
      for (final entry in entries) {
        await store._db.runInsert(
          'INSERT INTO transactions(title, category, amount, occurred_at, source, source_hash) VALUES (?, ?, ?, ?, ?, ?)',
          [
            entry.$1,
            entry.$2,
            entry.$3,
            entry.$4.millisecondsSinceEpoch,
            'seed',
            null
          ],
        );
      }
    }

    final taskCount = await store._countRows('tasks');
    if (taskCount == 0) {
      final nowMs = DateTime.now().millisecondsSinceEpoch;
      await store._db.runInsert(
        'INSERT INTO tasks(title, description, completed, due_at, priority) VALUES (?, ?, ?, ?, ?)',
        [
          'Prepare monthly spending review',
          'Review top spending categories and action items.',
          0,
          nowMs,
          'high'
        ],
      );
      await store._db.runInsert(
        'INSERT INTO tasks(title, description, completed, due_at, priority) VALUES (?, ?, ?, ?, ?)',
        [
          'Submit transport expense report',
          'Send final report to finance.',
          1,
          nowMs,
          'medium'
        ],
      );
    }

    final incomeCount = await store._countRows('incomes');
    if (incomeCount == 0) {
      await store._db.runInsert(
        'INSERT INTO incomes(title, amount, received_at, source) VALUES (?, ?, ?, ?)',
        [
          'Salary',
          120000.0,
          DateTime.now()
              .subtract(const Duration(days: 5))
              .millisecondsSinceEpoch,
          'seed',
        ],
      );
    }

    final budgetCount = await store._countRows('budgets');
    if (budgetCount == 0) {
      await store._db.runInsert(
        'INSERT OR IGNORE INTO budgets(category, monthly_limit) VALUES (?, ?)',
        ['Food', 15000.0],
      );
      await store._db.runInsert(
        'INSERT OR IGNORE INTO budgets(category, monthly_limit) VALUES (?, ?)',
        ['Transport', 8000.0],
      );
    }
  }

  static Future<void> tryAddSourceHashColumn(AppDriftStore store) async {
    try {
      await store._db
          .runCustom('ALTER TABLE transactions ADD COLUMN source_hash TEXT');
    } catch (_) {
      return;
    }
  }

  static Future<void> tryAddTaskDescriptionColumn(AppDriftStore store) async {
    try {
      await store._db
          .runCustom('ALTER TABLE tasks ADD COLUMN description TEXT');
    } catch (_) {
      return;
    }
  }

  static Future<void> tryAddIncomesSourceColumn(AppDriftStore store) async {
    try {
      await store._db.runCustom(
        'ALTER TABLE incomes ADD COLUMN source TEXT NOT NULL DEFAULT \'manual\'',
      );
    } catch (_) {
      return;
    }
  }

  static Future<void> tryAddRecurringPriorityColumn(AppDriftStore store) async {
    try {
      await store._db.runCustom(
          'ALTER TABLE recurring_templates ADD COLUMN priority TEXT');
    } catch (_) {
      return;
    }
  }

  static Future<void> tryAddEventCompletedColumn(AppDriftStore store) async {
    try {
      await store._db.runCustom(
        'ALTER TABLE events ADD COLUMN completed INTEGER NOT NULL DEFAULT 0',
      );
    } catch (_) {
      return;
    }
  }

  static Future<void> tryAddEventPriorityColumn(AppDriftStore store) async {
    try {
      await store._db.runCustom(
        'ALTER TABLE events ADD COLUMN priority TEXT NOT NULL DEFAULT \'medium\'',
      );
    } catch (_) {
      return;
    }
  }

  static Future<void> tryAddEventTypeColumn(AppDriftStore store) async {
    try {
      await store._db.runCustom(
        'ALTER TABLE events ADD COLUMN event_type TEXT NOT NULL DEFAULT \'general\'',
      );
    } catch (_) {
      return;
    }
  }
}
