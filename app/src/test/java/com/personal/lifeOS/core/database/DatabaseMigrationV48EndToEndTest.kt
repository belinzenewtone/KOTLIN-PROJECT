package com.personal.lifeOS.core.database

import android.app.Application
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DatabaseMigrationV48EndToEndTest {
    @Test
    fun `migration chain 4 to 8 preserves core data and creates new tables`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbName = "migration-v48-chain-${System.currentTimeMillis()}.db"
        val dbFile = context.getDatabasePath(dbName)
        if (dbFile.exists()) {
            dbFile.delete()
        }

        val callback =
            object : SupportSQLiteOpenHelper.Callback(4) {
                override fun onCreate(db: SupportSQLiteDatabase) = Unit

                override fun onUpgrade(
                    db: SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int,
                ) = Unit
            }

        val config =
            SupportSQLiteOpenHelper.Configuration
                .builder(context)
                .name(dbName)
                .callback(callback)
                .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase

        createVersion4Schema(db)
        seedVersion4Data(db)

        DatabaseMigrations.MIGRATION_4_5.migrate(db)
        DatabaseMigrations.MIGRATION_5_6.migrate(db)
        DatabaseMigrations.MIGRATION_6_7.migrate(db)
        DatabaseMigrations.MIGRATION_7_8.migrate(db)

        assertEquals(1, countRows(db, "transactions", "user_id = 'user-1'"))
        assertEquals(1, countRows(db, "tasks", "user_id = 'user-1'"))
        assertEquals(1, countRows(db, "events", "user_id = 'user-1'"))
        assertEquals(1, countRows(db, "merchant_categories", "user_id = 'user-1'"))

        assertTrue(columnExists(db, "transactions", "sync_state"))
        assertTrue(columnExists(db, "tasks", "record_source"))
        assertTrue(columnExists(db, "events", "revision"))
        assertTrue(columnExists(db, "merchant_categories", "deleted_at"))

        assertTrue(tableExists(db, "sync_jobs"))
        assertTrue(tableExists(db, "import_audit"))
        assertTrue(tableExists(db, "assistant_conversations"))
        assertTrue(tableExists(db, "assistant_messages"))
        assertTrue(tableExists(db, "insight_cards"))
        assertTrue(tableExists(db, "review_snapshots"))
        assertTrue(tableExists(db, "app_update_info"))
        assertTrue(tableExists(db, "export_history"))

        assertTrue(indexExists(db, "index_export_history_user_id"))
        assertTrue(indexExists(db, "index_export_history_exported_at"))
        assertTrue(indexExists(db, "index_export_history_status"))

        db.close()
        helper.close()
        dbFile.delete()
    }

    private fun createVersion4Schema(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS transactions (
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
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS tasks (
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
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS events (
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
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS merchant_categories (
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
    }

    private fun seedVersion4Data(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            INSERT INTO transactions (
                id, amount, merchant, category, date, source, transaction_type, mpesa_code, raw_sms, created_at, user_id
            ) VALUES (
                101, 1200.0, 'NAIVAS', 'Groceries', 1700000000000, 'MPESA', 'PAID', 'QH12345678', 'raw', 1700000000000, 'user-1'
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO tasks (
                id, title, description, priority, deadline, status, completed_at, created_at, user_id
            ) VALUES (
                201, 'Review budget', 'Monthly review', 'HIGH', 1700000200000, 'PENDING', NULL, 1700000000000, 'user-1'
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO events (
                id, title, description, date, end_date, type, importance,
                has_reminder, reminder_minutes_before, created_at, user_id
            ) VALUES (
                301, 'Finance check-in', 'Weekly sync', 1700000300000, NULL, 'PERSONAL', 'NEUTRAL',
                1, 15, 1700000000000, 'user-1'
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO merchant_categories (
                id, merchant, category, confidence, userCorrected, user_id
            ) VALUES (
                401, 'NAIVAS', 'Groceries', 1.0, 0, 'user-1'
            )
            """.trimIndent(),
        )
    }

    private fun countRows(
        db: SupportSQLiteDatabase,
        tableName: String,
        whereClause: String,
    ): Int {
        db.query("SELECT COUNT(*) AS count FROM $tableName WHERE $whereClause").use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getInt(0)
            }
        }
        return 0
    }

    private fun tableExists(
        db: SupportSQLiteDatabase,
        tableName: String,
    ): Boolean {
        db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
            arrayOf(tableName),
        ).use { cursor ->
            return cursor.moveToFirst()
        }
    }

    private fun columnExists(
        db: SupportSQLiteDatabase,
        tableName: String,
        columnName: String,
    ): Boolean {
        db.query("PRAGMA table_info($tableName)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == columnName) return true
            }
        }
        return false
    }

    private fun indexExists(
        db: SupportSQLiteDatabase,
        indexName: String,
    ): Boolean {
        db.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND name=?",
            arrayOf(indexName),
        ).use { cursor ->
            return cursor.moveToFirst()
        }
    }
}
