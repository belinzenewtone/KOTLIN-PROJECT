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
class DatabaseMigrationV78Test {
    @Test
    fun `migration 7 to 8 creates export history table and indexes`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbName = "migration-v78-${System.currentTimeMillis()}.db"
        val dbFile = context.getDatabasePath(dbName)
        if (dbFile.exists()) {
            dbFile.delete()
        }

        val callback =
            object : SupportSQLiteOpenHelper.Callback(7) {
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

        DatabaseMigrations.MIGRATION_7_8.migrate(db)

        assertTrue(tableExists(db, "export_history"))
        assertTrue(columnExists(db, "export_history", "domain_scope"))
        assertTrue(columnExists(db, "export_history", "is_encrypted"))
        assertTrue(indexExists(db, "index_export_history_user_id"))
        assertTrue(indexExists(db, "index_export_history_exported_at"))
        assertTrue(indexExists(db, "index_export_history_status"))
        assertTrue(indexExists(db, "index_export_history_format"))
        assertTrue(indexExists(db, "index_export_history_domain_scope"))

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
