package com.personal.lifeOS.core.database

import android.app.Application
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DatabaseMigrationV1415Test {
    @Test
    fun `migration 14 to 15 recreates spending views with outflow filter`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbName = "migration-v1415-${System.currentTimeMillis()}.db"
        val dbFile = context.getDatabasePath(dbName)
        if (dbFile.exists()) {
            dbFile.delete()
        }

        val callback =
            object : SupportSQLiteOpenHelper.Callback(14) {
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

        createVersion14TransactionsSchema(db)
        createVersion14Views(db)

        DatabaseMigrations.MIGRATION_14_15.migrate(db)
        val outflowTypes =
            "'SENT', 'AIRTIME', 'PAYBILL', 'BUY_GOODS', " +
                "'WITHDRAW', 'PAID', 'WITHDRAWN'"

        assertEquals(
            normalizeSql(
                "CREATE VIEW `daily_spend` AS " +
                    "SELECT user_id, strftime('%Y-%m-%d', date / 1000, 'unixepoch', 'localtime') AS spend_date, " +
                    "SUM(amount) AS total_amount, COUNT(*) AS tx_count FROM transactions " +
                    "WHERE deleted_at IS NULL AND UPPER(transaction_type) IN ($outflowTypes) " +
                    "GROUP BY user_id, strftime('%Y-%m-%d', date / 1000, 'unixepoch', 'localtime')",
            ),
            normalizeSql(viewSql(db, "daily_spend")),
        )
        assertEquals(
            normalizeSql(
                "CREATE VIEW `monthly_spend` AS " +
                    "SELECT user_id, strftime('%Y-%m', date / 1000, 'unixepoch', 'localtime') AS spend_month, " +
                    "SUM(amount) AS total_amount, COUNT(*) AS tx_count FROM transactions " +
                    "WHERE deleted_at IS NULL AND UPPER(transaction_type) IN ($outflowTypes) " +
                    "GROUP BY user_id, strftime('%Y-%m', date / 1000, 'unixepoch', 'localtime')",
            ),
            normalizeSql(viewSql(db, "monthly_spend")),
        )

        db.close()
        helper.close()
        dbFile.delete()
    }

    private fun createVersion14TransactionsSchema(db: SupportSQLiteDatabase) {
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
                source_hash TEXT,
                raw_sms TEXT,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                sync_state TEXT NOT NULL,
                record_source TEXT NOT NULL,
                deleted_at INTEGER,
                revision INTEGER NOT NULL,
                user_id TEXT NOT NULL,
                inferred_category TEXT,
                inference_source TEXT,
                semantic_hash TEXT,
                PRIMARY KEY(user_id, id)
            )
            """.trimIndent(),
        )
    }

    private fun createVersion14Views(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE VIEW daily_spend AS
            SELECT user_id,
                   strftime('%Y-%m-%d', date / 1000, 'unixepoch', 'localtime') AS spend_date,
                   SUM(amount) AS total_amount,
                   COUNT(*) AS tx_count
            FROM transactions
            WHERE deleted_at IS NULL
            GROUP BY user_id, strftime('%Y-%m-%d', date / 1000, 'unixepoch', 'localtime')
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE VIEW monthly_spend AS
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

    private fun viewSql(
        db: SupportSQLiteDatabase,
        viewName: String,
    ): String? {
        db.query(
            "SELECT sql FROM sqlite_master WHERE type='view' AND name=?",
            arrayOf(viewName),
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }

    private fun normalizeSql(sql: String?): String? = sql?.replace("\\s+".toRegex(), " ")?.trim()
}
