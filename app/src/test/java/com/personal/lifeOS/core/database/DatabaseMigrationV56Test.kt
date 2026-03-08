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
class DatabaseMigrationV56Test {
    @Test
    fun `migration 5 to 6 adds event status column and index`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbName = "migration-v56-${System.currentTimeMillis()}.db"
        val dbFile = context.getDatabasePath(dbName)
        if (dbFile.exists()) {
            dbFile.delete()
        }

        val callback =
            object : SupportSQLiteOpenHelper.Callback(5) {
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

        DatabaseMigrations.MIGRATION_5_6.migrate(db)

        assertTrue(columnExists(db, "events", "status"))
        assertTrue(indexExists(db, "index_events_status"))

        db.close()
        helper.close()
        dbFile.delete()
    }

    private fun columnExists(
        db: SupportSQLiteDatabase,
        tableName: String,
        columnName: String,
    ): Boolean {
        db.query("PRAGMA table_info($tableName)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == columnName) {
                    return true
                }
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
