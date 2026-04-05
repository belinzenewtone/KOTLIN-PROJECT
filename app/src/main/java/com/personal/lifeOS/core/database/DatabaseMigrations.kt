@file:Suppress("MaxLineLength")

package com.personal.lifeOS.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    private fun hasColumn(
        database: SupportSQLiteDatabase,
        tableName: String,
        columnName: String,
    ): Boolean {
        database.query("PRAGMA table_info(`$tableName`)").use { cursor ->
            val nameColumnIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (nameColumnIndex >= 0 && cursor.getString(nameColumnIndex) == columnName) {
                    return true
                }
            }
        }
        return false
    }

    @Suppress("MaxLineLength")
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
                        id, amount, merchant, category, date, source, transaction_type,
                        mpesa_code, raw_sms, created_at, user_id
                    )
                    SELECT
                        id, amount, merchant, category, date, source, transaction_type,
                        mpesa_code, raw_sms, created_at, user_id
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
                        id, title, description, date, end_date, type, importance,
                        has_reminder, reminder_minutes_before, created_at, user_id
                    )
                    SELECT
                        id, title, description, date, end_date, type, importance,
                        has_reminder, reminder_minutes_before, created_at, user_id
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

    val MIGRATION_6_7: Migration =
        object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE transactions ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE transactions ADD COLUMN sync_state TEXT NOT NULL DEFAULT 'LOCAL_ONLY'")
                database.execSQL("ALTER TABLE transactions ADD COLUMN record_source TEXT NOT NULL DEFAULT 'SMS'")
                database.execSQL("ALTER TABLE transactions ADD COLUMN deleted_at INTEGER")
                database.execSQL("ALTER TABLE transactions ADD COLUMN revision INTEGER NOT NULL DEFAULT 0")

                database.execSQL("ALTER TABLE tasks ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE tasks ADD COLUMN sync_state TEXT NOT NULL DEFAULT 'LOCAL_ONLY'")
                database.execSQL("ALTER TABLE tasks ADD COLUMN record_source TEXT NOT NULL DEFAULT 'MANUAL'")
                database.execSQL("ALTER TABLE tasks ADD COLUMN deleted_at INTEGER")
                database.execSQL("ALTER TABLE tasks ADD COLUMN revision INTEGER NOT NULL DEFAULT 0")

                database.execSQL("ALTER TABLE events ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE events ADD COLUMN sync_state TEXT NOT NULL DEFAULT 'LOCAL_ONLY'")
                database.execSQL("ALTER TABLE events ADD COLUMN record_source TEXT NOT NULL DEFAULT 'MANUAL'")
                database.execSQL("ALTER TABLE events ADD COLUMN deleted_at INTEGER")
                database.execSQL("ALTER TABLE events ADD COLUMN revision INTEGER NOT NULL DEFAULT 0")

                database.execSQL("ALTER TABLE budgets ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE budgets ADD COLUMN sync_state TEXT NOT NULL DEFAULT 'LOCAL_ONLY'")
                database.execSQL("ALTER TABLE budgets ADD COLUMN record_source TEXT NOT NULL DEFAULT 'MANUAL'")
                database.execSQL("ALTER TABLE budgets ADD COLUMN deleted_at INTEGER")
                database.execSQL("ALTER TABLE budgets ADD COLUMN revision INTEGER NOT NULL DEFAULT 0")

                database.execSQL("ALTER TABLE incomes ADD COLUMN created_at INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE incomes ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE incomes ADD COLUMN sync_state TEXT NOT NULL DEFAULT 'LOCAL_ONLY'")
                database.execSQL("ALTER TABLE incomes ADD COLUMN record_source TEXT NOT NULL DEFAULT 'MANUAL'")
                database.execSQL("ALTER TABLE incomes ADD COLUMN deleted_at INTEGER")
                database.execSQL("ALTER TABLE incomes ADD COLUMN revision INTEGER NOT NULL DEFAULT 0")

                database.execSQL("ALTER TABLE recurring_rules ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE recurring_rules ADD COLUMN sync_state TEXT NOT NULL DEFAULT 'LOCAL_ONLY'")
                database.execSQL(
                    "ALTER TABLE recurring_rules ADD COLUMN record_source TEXT NOT NULL DEFAULT 'RECURRING'",
                )
                database.execSQL("ALTER TABLE recurring_rules ADD COLUMN deleted_at INTEGER")
                database.execSQL("ALTER TABLE recurring_rules ADD COLUMN revision INTEGER NOT NULL DEFAULT 0")

                database.execSQL("ALTER TABLE merchant_categories ADD COLUMN created_at INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE merchant_categories ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0")
                database.execSQL(
                    "ALTER TABLE merchant_categories ADD COLUMN sync_state TEXT NOT NULL DEFAULT 'LOCAL_ONLY'",
                )
                database.execSQL(
                    "ALTER TABLE merchant_categories ADD COLUMN record_source TEXT NOT NULL DEFAULT 'SYSTEM'",
                )
                database.execSQL("ALTER TABLE merchant_categories ADD COLUMN deleted_at INTEGER")
                database.execSQL("ALTER TABLE merchant_categories ADD COLUMN revision INTEGER NOT NULL DEFAULT 0")

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sync_jobs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        job_type TEXT NOT NULL,
                        entity_type TEXT NOT NULL,
                        entity_id TEXT NOT NULL,
                        payload TEXT NOT NULL,
                        status TEXT NOT NULL,
                        attempt_count INTEGER NOT NULL,
                        last_error TEXT,
                        next_run_at INTEGER NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_sync_jobs_status ON sync_jobs(status)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_sync_jobs_next_run_at ON sync_jobs(next_run_at)")
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_sync_jobs_entity_type_entity_id " +
                        "ON sync_jobs(entity_type, entity_id)",
                )

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS import_audit (
                        id INTEGER NOT NULL,
                        user_id TEXT NOT NULL,
                        raw_message TEXT NOT NULL,
                        mpesa_code TEXT,
                        amount REAL,
                        merchant TEXT,
                        outcome TEXT NOT NULL,
                        failure_reason TEXT,
                        imported_at INTEGER NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        PRIMARY KEY(user_id, id)
                    )
                    """.trimIndent(),
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_import_audit_user_id ON import_audit(user_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_import_audit_outcome ON import_audit(outcome)")
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_import_audit_imported_at ON import_audit(imported_at)",
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_import_audit_mpesa_code ON import_audit(mpesa_code)")

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS assistant_conversations (
                        id INTEGER NOT NULL,
                        user_id TEXT NOT NULL,
                        title TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        sync_state TEXT NOT NULL,
                        record_source TEXT NOT NULL,
                        deleted_at INTEGER,
                        revision INTEGER NOT NULL,
                        PRIMARY KEY(user_id, id)
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_assistant_conversations_user_id " +
                        "ON assistant_conversations(user_id)",
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_assistant_conversations_updated_at " +
                        "ON assistant_conversations(updated_at)",
                )

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS assistant_messages (
                        id INTEGER NOT NULL,
                        user_id TEXT NOT NULL,
                        conversation_id INTEGER NOT NULL,
                        role TEXT NOT NULL,
                        content TEXT NOT NULL,
                        action_payload TEXT,
                        is_preview INTEGER NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        sync_state TEXT NOT NULL,
                        record_source TEXT NOT NULL,
                        deleted_at INTEGER,
                        revision INTEGER NOT NULL,
                        PRIMARY KEY(user_id, id)
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_assistant_messages_user_id ON assistant_messages(user_id)",
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_assistant_messages_conversation_id " +
                        "ON assistant_messages(conversation_id)",
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_assistant_messages_created_at ON assistant_messages(created_at)",
                )

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS insight_cards (
                        id INTEGER NOT NULL,
                        user_id TEXT NOT NULL,
                        kind TEXT NOT NULL,
                        title TEXT NOT NULL,
                        body TEXT NOT NULL,
                        confidence REAL,
                        is_ai_generated INTEGER NOT NULL,
                        fresh_until INTEGER,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        sync_state TEXT NOT NULL,
                        record_source TEXT NOT NULL,
                        deleted_at INTEGER,
                        revision INTEGER NOT NULL,
                        PRIMARY KEY(user_id, id)
                    )
                    """.trimIndent(),
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_insight_cards_user_id ON insight_cards(user_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_insight_cards_kind ON insight_cards(kind)")
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_insight_cards_created_at ON insight_cards(created_at)",
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_insight_cards_fresh_until ON insight_cards(fresh_until)",
                )

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS review_snapshots (
                        id INTEGER NOT NULL,
                        user_id TEXT NOT NULL,
                        period_start INTEGER NOT NULL,
                        period_end INTEGER NOT NULL,
                        payload TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        sync_state TEXT NOT NULL,
                        record_source TEXT NOT NULL,
                        deleted_at INTEGER,
                        revision INTEGER NOT NULL,
                        PRIMARY KEY(user_id, id)
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_review_snapshots_user_id ON review_snapshots(user_id)",
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_review_snapshots_period_start ON review_snapshots(period_start)",
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_review_snapshots_period_end ON review_snapshots(period_end)",
                )

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS app_update_info (
                        id INTEGER NOT NULL,
                        user_id TEXT NOT NULL,
                        version_code INTEGER NOT NULL,
                        version_name TEXT,
                        is_required INTEGER NOT NULL,
                        download_url TEXT,
                        checksum_sha256 TEXT,
                        checked_at INTEGER NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        PRIMARY KEY(user_id, id)
                    )
                    """.trimIndent(),
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_app_update_info_user_id ON app_update_info(user_id)")
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_app_update_info_checked_at ON app_update_info(checked_at)",
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_app_update_info_version_code ON app_update_info(version_code)",
                )

                val now = System.currentTimeMillis()
                database.execSQL("UPDATE transactions SET created_at = COALESCE(created_at, $now), updated_at = $now")
                database.execSQL("UPDATE tasks SET created_at = COALESCE(created_at, $now), updated_at = $now")
                database.execSQL("UPDATE events SET created_at = COALESCE(created_at, $now), updated_at = $now")
                database.execSQL("UPDATE budgets SET created_at = COALESCE(created_at, $now), updated_at = $now")
                database.execSQL("UPDATE incomes SET created_at = COALESCE(created_at, $now), updated_at = $now")
                database.execSQL(
                    "UPDATE recurring_rules SET created_at = COALESCE(created_at, $now), updated_at = $now",
                )
                database.execSQL(
                    "UPDATE merchant_categories SET created_at = COALESCE(created_at, $now), updated_at = $now",
                )
            }
        }

    val MIGRATION_7_8: Migration =
        object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS export_history (
                        id INTEGER NOT NULL,
                        user_id TEXT NOT NULL,
                        format TEXT NOT NULL,
                        domain_scope TEXT NOT NULL,
                        date_from INTEGER,
                        date_to INTEGER,
                        file_path TEXT,
                        item_count INTEGER NOT NULL,
                        is_encrypted INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        error_message TEXT,
                        exported_at INTEGER NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        PRIMARY KEY(user_id, id)
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_export_history_user_id ON export_history(user_id)",
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_export_history_exported_at ON export_history(exported_at)",
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_export_history_status ON export_history(status)",
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_export_history_format ON export_history(format)",
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_export_history_domain_scope ON export_history(domain_scope)",
                )
            }
        }

    val MIGRATION_8_9: Migration =
        object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                if (!hasColumn(database, "transactions", "source_hash")) {
                    database.execSQL("ALTER TABLE transactions ADD COLUMN source_hash TEXT")
                }
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_transactions_mpesa_code ON transactions(mpesa_code)",
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_transactions_source_hash ON transactions(source_hash)",
                )
            }
        }

    val MIGRATION_9_10: Migration =
        object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // ── Add CategoryInferenceEngine columns to transactions ────────
                if (!hasColumn(database, "transactions", "inferred_category")) {
                    database.execSQL("ALTER TABLE transactions ADD COLUMN inferred_category TEXT")
                }
                if (!hasColumn(database, "transactions", "inference_source")) {
                    database.execSQL("ALTER TABLE transactions ADD COLUMN inference_source TEXT")
                }

                // ── Create fuliza_loans table ─────────────────────────────────
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS fuliza_loans (
                        id INTEGER NOT NULL,
                        draw_code TEXT NOT NULL,
                        draw_amount_kes REAL NOT NULL,
                        total_repaid_kes REAL NOT NULL DEFAULT 0.0,
                        status TEXT NOT NULL DEFAULT 'OPEN',
                        draw_date INTEGER NOT NULL,
                        last_repayment_date INTEGER,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        user_id TEXT NOT NULL,
                        PRIMARY KEY(user_id, id)
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_fuliza_loans_user_id ON fuliza_loans(user_id)",
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_fuliza_loans_user_id_draw_code " +
                        "ON fuliza_loans(user_id, draw_code)",
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_fuliza_loans_status ON fuliza_loans(status)",
                )
            }
        }

    val MIGRATION_10_11: Migration =
        object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // semantic_hash on transactions
                if (!hasColumn(database, "transactions", "semantic_hash")) {
                    database.execSQL("ALTER TABLE transactions ADD COLUMN semantic_hash TEXT")
                }
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_transactions_semantic_hash ON transactions(semantic_hash)"
                )
                // confidence_score on import_audit
                if (!hasColumn(database, "import_audit", "confidence_score")) {
                    database.execSQL(
                        "ALTER TABLE import_audit ADD COLUMN confidence_score REAL NOT NULL DEFAULT 0.0"
                    )
                }
                // paybill_registry table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS paybill_registry (
                        user_id TEXT NOT NULL,
                        paybill_number TEXT NOT NULL,
                        display_name TEXT NOT NULL,
                        last_seen_at INTEGER NOT NULL,
                        usage_count INTEGER NOT NULL DEFAULT 1,
                        last_amount_kes REAL NOT NULL DEFAULT 0.0,
                        PRIMARY KEY(user_id, paybill_number)
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS index_paybill_registry_user_id ON paybill_registry(user_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_paybill_registry_usage_count ON paybill_registry(usage_count)")
            }
        }

    val MIGRATION_11_12: Migration =
        object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP VIEW IF EXISTS daily_spend")
                database.execSQL("DROP VIEW IF EXISTS monthly_spend")
                database.execSQL(
                    """
                    CREATE VIEW IF NOT EXISTS `daily_spend` AS
                    SELECT user_id,
                           strftime('%Y-%m-%d', date / 1000, 'unixepoch', 'localtime') AS spend_date,
                           SUM(amount) AS total_amount,
                           COUNT(*) AS tx_count
                    FROM transactions
                    WHERE deleted_at IS NULL
                    GROUP BY user_id, strftime('%Y-%m-%d', date / 1000, 'unixepoch', 'localtime')
                    """.trimIndent(),
                )
                database.execSQL(
                    """
                    CREATE VIEW IF NOT EXISTS `monthly_spend` AS
                    SELECT user_id,
                           strftime('%Y-%m', date / 1000, 'unixepoch', 'localtime') AS spend_month,
                           SUM(amount) AS total_amount,
                           COUNT(*) AS tx_count
                    FROM transactions
                    WHERE deleted_at IS NULL
                    GROUP BY user_id, strftime('%Y-%m', date / 1000, 'unixepoch', 'localtime')
                    """.trimIndent(),
                )
            }
        }

    val MIGRATION_12_13: Migration =
        object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP VIEW IF EXISTS daily_spend")
                database.execSQL("DROP VIEW IF EXISTS monthly_spend")
                database.execSQL(
                    """
                    CREATE VIEW `daily_spend` AS
                    SELECT user_id,
                           strftime('%Y-%m-%d', date / 1000, 'unixepoch', 'localtime') AS spend_date,
                           SUM(amount) AS total_amount,
                           COUNT(*) AS tx_count
                    FROM transactions
                    WHERE deleted_at IS NULL
                    GROUP BY user_id, strftime('%Y-%m-%d', date / 1000, 'unixepoch', 'localtime')
                    """.trimIndent(),
                )
                database.execSQL(
                    """
                    CREATE VIEW `monthly_spend` AS
                    SELECT user_id,
                           strftime('%Y-%m', date / 1000, 'unixepoch', 'localtime') AS spend_month,
                           SUM(amount) AS total_amount,
                           COUNT(*) AS tx_count
                    FROM transactions
                    WHERE deleted_at IS NULL
                    GROUP BY user_id, strftime('%Y-%m', date / 1000, 'unixepoch', 'localtime')
                    """.trimIndent(),
                )
            }
        }

    // Fixes Room @DatabaseView whitespace mismatch (v13 views were created with trimIndent() SQL
    // that did not match the literal string Room derives from the @DatabaseView annotation).
    // Views are recreated here with single-line SQL that matches the annotation value exactly.
    val MIGRATION_13_14: Migration =
        object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP VIEW IF EXISTS `daily_spend`")
                database.execSQL("DROP VIEW IF EXISTS `monthly_spend`")
                // SQL must be IDENTICAL (character-for-character) to the value in @DatabaseView.
                database.execSQL(
                    "CREATE VIEW `daily_spend` AS SELECT user_id, strftime('%Y-%m-%d', date / 1000, 'unixepoch', 'localtime') AS spend_date, SUM(amount) AS total_amount, COUNT(*) AS tx_count FROM transactions WHERE deleted_at IS NULL GROUP BY user_id, strftime('%Y-%m-%d', date / 1000, 'unixepoch', 'localtime')",
                )
                database.execSQL(
                    "CREATE VIEW `monthly_spend` AS SELECT user_id, strftime('%Y-%m', date / 1000, 'unixepoch', 'localtime') AS spend_month, SUM(amount) AS total_amount, COUNT(*) AS tx_count FROM transactions WHERE deleted_at IS NULL GROUP BY user_id, strftime('%Y-%m', date / 1000, 'unixepoch', 'localtime')",
                )
            }
        }

    // Restrict spending views to outflow transaction types only.
    val MIGRATION_14_15: Migration =
        object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP VIEW IF EXISTS `daily_spend`")
                database.execSQL("DROP VIEW IF EXISTS `monthly_spend`")
                // SQL must be IDENTICAL (character-for-character) to the value in @DatabaseView.
                database.execSQL(
                    "CREATE VIEW `daily_spend` AS SELECT user_id, strftime('%Y-%m-%d', date / 1000, 'unixepoch', 'localtime') AS spend_date, SUM(amount) AS total_amount, COUNT(*) AS tx_count FROM transactions WHERE deleted_at IS NULL AND UPPER(transaction_type) IN ('SENT', 'AIRTIME', 'PAYBILL', 'BUY_GOODS', 'WITHDRAW', 'PAID', 'WITHDRAWN') GROUP BY user_id, strftime('%Y-%m-%d', date / 1000, 'unixepoch', 'localtime')",
                )
                database.execSQL(
                    "CREATE VIEW `monthly_spend` AS SELECT user_id, strftime('%Y-%m', date / 1000, 'unixepoch', 'localtime') AS spend_month, SUM(amount) AS total_amount, COUNT(*) AS tx_count FROM transactions WHERE deleted_at IS NULL AND UPPER(transaction_type) IN ('SENT', 'AIRTIME', 'PAYBILL', 'BUY_GOODS', 'WITHDRAW', 'PAID', 'WITHDRAWN') GROUP BY user_id, strftime('%Y-%m', date / 1000, 'unixepoch', 'localtime')",
                )
            }
        }
}
