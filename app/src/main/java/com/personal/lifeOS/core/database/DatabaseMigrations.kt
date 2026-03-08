package com.personal.lifeOS.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_1_2: Migration =
        object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE events ADD COLUMN importance TEXT NOT NULL DEFAULT 'NEUTRAL'")
            }
        }

    val MIGRATION_2_3: Migration =
        object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE transactions ADD COLUMN user_id TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE tasks ADD COLUMN user_id TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE events ADD COLUMN user_id TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE merchant_categories ADD COLUMN user_id TEXT NOT NULL DEFAULT ''")

                database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_user_id ON transactions(user_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_user_id ON tasks(user_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_events_user_id ON events(user_id)")

                database.execSQL("DROP INDEX IF EXISTS index_merchant_categories_merchant")
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_merchant_categories_user_id ON merchant_categories(user_id)",
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_merchant_categories_user_id_merchant " +
                        "ON merchant_categories(user_id, merchant)",
                )
            }
        }

    val MIGRATION_3_4: Migration =
        object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS transactions_new (
                        id INTEGER NOT NULL,
                        amount REAL NOT NULL,
                        merchant TEXT NOT NULL,
                        category TEXT NOT NULL,
                        date INTEGER NOT NULL,
                        source TEXT NOT NULL,
                        transaction_type TEXT NOT NULL,
                        mpesa_code TEXT,
                        raw_sms TEXT,
                        created_at INTEGER NOT NULL,
                        user_id TEXT NOT NULL,
                        PRIMARY KEY(user_id, id)
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    """
                    INSERT INTO transactions_new (
                        id, amount, merchant, category, date, source, transaction_type, mpesa_code, raw_sms, created_at, user_id
                    )
                    SELECT
                        id, amount, merchant, category, date, source, transaction_type, mpesa_code, raw_sms, created_at, user_id
                    FROM transactions
                    """.trimIndent(),
                )
                database.execSQL("DROP TABLE transactions")
                database.execSQL("ALTER TABLE transactions_new RENAME TO transactions")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_user_id ON transactions(user_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_date ON transactions(date)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_category ON transactions(category)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_merchant ON transactions(merchant)")

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS tasks_new (
                        id INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        priority TEXT NOT NULL,
                        deadline INTEGER,
                        status TEXT NOT NULL,
                        completed_at INTEGER,
                        created_at INTEGER NOT NULL,
                        user_id TEXT NOT NULL,
                        PRIMARY KEY(user_id, id)
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    """
                    INSERT INTO tasks_new (
                        id, title, description, priority, deadline, status, completed_at, created_at, user_id
                    )
                    SELECT
                        id, title, description, priority, deadline, status, completed_at, created_at, user_id
                    FROM tasks
                    """.trimIndent(),
                )
                database.execSQL("DROP TABLE tasks")
                database.execSQL("ALTER TABLE tasks_new RENAME TO tasks")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_user_id ON tasks(user_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_deadline ON tasks(deadline)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_status ON tasks(status)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_priority ON tasks(priority)")

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS events_new (
                        id INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        date INTEGER NOT NULL,
                        end_date INTEGER,
                        type TEXT NOT NULL,
                        importance TEXT NOT NULL,
                        has_reminder INTEGER NOT NULL,
                        reminder_minutes_before INTEGER NOT NULL,
                        created_at INTEGER NOT NULL,
                        user_id TEXT NOT NULL,
                        PRIMARY KEY(user_id, id)
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    """
                    INSERT INTO events_new (
                        id, title, description, date, end_date, type, importance, has_reminder, reminder_minutes_before, created_at, user_id
                    )
                    SELECT
                        id, title, description, date, end_date, type, importance, has_reminder, reminder_minutes_before, created_at, user_id
                    FROM events
                    """.trimIndent(),
                )
                database.execSQL("DROP TABLE events")
                database.execSQL("ALTER TABLE events_new RENAME TO events")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_events_user_id ON events(user_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_events_date ON events(date)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_events_type ON events(type)")

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS merchant_categories_new (
                        id INTEGER NOT NULL,
                        merchant TEXT NOT NULL,
                        category TEXT NOT NULL,
                        confidence REAL NOT NULL,
                        userCorrected INTEGER NOT NULL,
                        user_id TEXT NOT NULL,
                        PRIMARY KEY(user_id, id)
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    """
                    INSERT INTO merchant_categories_new (
                        id, merchant, category, confidence, userCorrected, user_id
                    )
                    SELECT
                        id, merchant, category, confidence, userCorrected, user_id
                    FROM merchant_categories
                    """.trimIndent(),
                )
                database.execSQL("DROP TABLE merchant_categories")
                database.execSQL("ALTER TABLE merchant_categories_new RENAME TO merchant_categories")
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_merchant_categories_user_id ON merchant_categories(user_id)",
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_merchant_categories_user_id_merchant " +
                        "ON merchant_categories(user_id, merchant)",
                )
            }
        }

    val MIGRATION_4_5: Migration =
        object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS budgets (
                        id INTEGER NOT NULL,
                        category TEXT NOT NULL,
                        limit_amount REAL NOT NULL,
                        period TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        user_id TEXT NOT NULL,
                        PRIMARY KEY(user_id, id)
                    )
                    """.trimIndent(),
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_budgets_user_id ON budgets(user_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_budgets_category ON budgets(category)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_budgets_period ON budgets(period)")

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS incomes (
                        id INTEGER NOT NULL,
                        amount REAL NOT NULL,
                        source TEXT NOT NULL,
                        date INTEGER NOT NULL,
                        note TEXT NOT NULL,
                        is_recurring INTEGER NOT NULL,
                        user_id TEXT NOT NULL,
                        PRIMARY KEY(user_id, id)
                    )
                    """.trimIndent(),
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_incomes_user_id ON incomes(user_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_incomes_date ON incomes(date)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_incomes_source ON incomes(source)")

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS recurring_rules (
                        id INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        type TEXT NOT NULL,
                        cadence TEXT NOT NULL,
                        next_run_at INTEGER NOT NULL,
                        amount REAL,
                        enabled INTEGER NOT NULL,
                        created_at INTEGER NOT NULL,
                        user_id TEXT NOT NULL,
                        PRIMARY KEY(user_id, id)
                    )
                    """.trimIndent(),
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_recurring_rules_user_id ON recurring_rules(user_id)")
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_recurring_rules_next_run_at ON recurring_rules(next_run_at)",
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_recurring_rules_enabled ON recurring_rules(enabled)")
            }
        }

    val MIGRATION_5_6: Migration =
        object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE events ADD COLUMN status TEXT NOT NULL DEFAULT 'PENDING'")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_events_status ON events(status)")
            }
        }
}
