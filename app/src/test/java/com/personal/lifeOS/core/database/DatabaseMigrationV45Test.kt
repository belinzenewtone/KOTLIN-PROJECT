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
class DatabaseMigrationV45Test {
    @Test
    fun `migration 4 to 5 creates parity tables and indexes`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbName = "migration-v45-${System.currentTimeMillis()}.db"
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
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(callback)
                .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase

        DatabaseMigrations.MIGRATION_4_5.migrate(db)

        assertTrue(tableExists(db, "budgets"))
        assertTrue(tableExists(db, "incomes"))
        assertTrue(tableExists(db, "recurring_rules"))
        assertTrue(indexExists(db, "index_budgets_user_id"))
        assertTrue(indexExists(db, "index_incomes_user_id"))
        assertTrue(indexExists(db, "index_recurring_rules_enabled"))

        db.close()
        helper.close()
        dbFile.delete()
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
