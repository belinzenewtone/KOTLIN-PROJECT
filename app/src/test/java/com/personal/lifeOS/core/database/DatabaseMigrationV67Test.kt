package com.personal.lifeOS.core.database

import android.app.Application
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DatabaseMigrationV67Test {
    @Test
    fun `migration 6 to 7 adds canonical metadata and core tables`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbName = "migration-v67-${System.currentTimeMillis()}.db"
        val dbFile = context.getDatabasePath(dbName)
        if (dbFile.exists()) {
            dbFile.delete()
        }

        val callback =
            object : SupportSQLiteOpenHelper.Callback(6) {
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

        createVersion6Schema(db)
        DatabaseMigrations.MIGRATION_6_7.migrate(db)

        assertTrue(columnExists(db, "transactions", "sync_state"))
        assertTrue(columnExists(db, "tasks", "record_source"))
        assertTrue(columnExists(db, "events", "revision"))
        assertTrue(columnExists(db, "budgets", "deleted_at"))
        assertTrue(columnExists(db, "incomes", "created_at"))
        assertTrue(columnExists(db, "recurring_rules", "updated_at"))
        assertTrue(columnExists(db, "merchant_categories", "sync_state"))

        assertTrue(tableExists(db, "sync_jobs"))
        assertTrue(tableExists(db, "import_audit"))
        assertTrue(tableExists(db, "assistant_conversations"))
        assertTrue(tableExists(db, "assistant_messages"))
        assertTrue(tableExists(db, "insight_cards"))
        assertTrue(tableExists(db, "review_snapshots"))
        assertTrue(tableExists(db, "app_update_info"))

        db.close()
        helper.close()
        dbFile.delete()
    }

    @Suppress("LongMethod")
    private fun createVersion6Schema(db: SupportSQLiteDatabase) {
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
                status TEXT NOT NULL,
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
        db.execSQL(
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
        db.execSQL(
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
}
