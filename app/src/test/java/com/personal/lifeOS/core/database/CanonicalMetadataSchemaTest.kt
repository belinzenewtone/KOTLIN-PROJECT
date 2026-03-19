package com.personal.lifeOS.core.database

import android.app.Application
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CanonicalMetadataSchemaTest {
    private lateinit var database: LifeOSDatabase
    private lateinit var sqlite: SupportSQLiteDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database =
            Room.inMemoryDatabaseBuilder(context, LifeOSDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        sqlite = database.openHelper.readableDatabase
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `mutable user-scoped tables include canonical sync metadata columns`() {
        val requiredColumns =
            setOf(
                "id",
                "user_id",
                "created_at",
                "updated_at",
                "sync_state",
                "record_source",
                "deleted_at",
                "revision",
            )
        val mutableTables =
            listOf(
                "transactions",
                "tasks",
                "events",
                "budgets",
                "incomes",
                "recurring_rules",
                "merchant_categories",
                "assistant_conversations",
                "assistant_messages",
                "insight_cards",
                "review_snapshots",
            )

        mutableTables.forEach { table ->
            val columnNames = columnNames(table)
            requiredColumns.forEach { column ->
                assertTrue(
                    "Expected column '$column' in table '$table', found: $columnNames",
                    columnNames.contains(column),
                )
            }
        }
    }

    private fun columnNames(tableName: String): Set<String> {
        val names = mutableSetOf<String>()
        sqlite.query("PRAGMA table_info($tableName)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                names += cursor.getString(nameIndex)
            }
        }
        return names
    }
}
