package com.personal.lifeOS.core.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.personal.lifeOS.core.database.dao.EventDao
import com.personal.lifeOS.core.database.dao.MerchantCategoryDao
import com.personal.lifeOS.core.database.dao.TaskDao
import com.personal.lifeOS.core.database.dao.TransactionDao
import com.personal.lifeOS.core.database.entity.EventEntity
import com.personal.lifeOS.core.database.entity.MerchantCategoryEntity
import com.personal.lifeOS.core.database.entity.TaskEntity
import com.personal.lifeOS.core.database.entity.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Syncs local Room database with Supabase cloud.
 * 
 * Supabase tables must match:
 * - transactions (id, amount, merchant, category, date, source, transaction_type, mpesa_code, raw_sms, created_at)
 * - tasks (id, title, description, priority, deadline, status, completed_at, created_at)
 * - events (id, title, description, date, end_date, type, has_reminder, reminder_minutes_before, created_at)
 * - merchant_categories (id, merchant, category, confidence, user_corrected)
 */
@Singleton
class CloudSyncService @Inject constructor(
    private val transactionDao: TransactionDao,
    private val taskDao: TaskDao,
    private val eventDao: EventDao,
    private val merchantCategoryDao: MerchantCategoryDao
) {
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Push all local data to Supabase (full sync up).
     * Returns number of tables synced successfully.
     */
    suspend fun pushToCloud(): SyncResult = withContext(Dispatchers.IO) {
        if (!ApiConfig.isSupabaseConfigured()) {
            return@withContext SyncResult(success = false, message = "Supabase not configured")
        }

        var synced = 0
        val errors = mutableListOf<String>()

        // Sync transactions
        try {
            val transactions = transactionDao.getAllTransactions().first()
            if (transactions.isNotEmpty()) {
                val success = upsertToSupabase("transactions", transactions)
                if (success) synced++ else errors.add("transactions")
            } else synced++
        } catch (e: Exception) {
            errors.add("transactions: ${e.message}")
        }

        // Sync tasks
        try {
            val tasks = taskDao.getAllTasks().first()
            if (tasks.isNotEmpty()) {
                val success = upsertToSupabase("tasks", tasks)
                if (success) synced++ else errors.add("tasks")
            } else synced++
        } catch (e: Exception) {
            errors.add("tasks: ${e.message}")
        }

        // Sync events
        try {
            val events = eventDao.getAllEvents().first()
            if (events.isNotEmpty()) {
                val success = upsertToSupabase("events", events)
                if (success) synced++ else errors.add("events")
            } else synced++
        } catch (e: Exception) {
            errors.add("events: ${e.message}")
        }

        // Sync merchant categories
        try {
            val merchants = merchantCategoryDao.getAll()
            if (merchants.isNotEmpty()) {
                val success = upsertToSupabase("merchant_categories", merchants)
                if (success) synced++ else errors.add("merchant_categories")
            } else synced++
        } catch (e: Exception) {
            errors.add("merchant_categories: ${e.message}")
        }

        SyncResult(
            success = errors.isEmpty(),
            message = if (errors.isEmpty()) "Synced $synced tables" else "Failed: ${errors.joinToString()}"
        )
    }

    /**
     * Pull data from Supabase and merge into local database.
     */
    suspend fun pullFromCloud(): SyncResult = withContext(Dispatchers.IO) {
        if (!ApiConfig.isSupabaseConfigured()) {
            return@withContext SyncResult(success = false, message = "Supabase not configured")
        }

        var synced = 0
        val errors = mutableListOf<String>()

        // Pull transactions
        try {
            val remote = fetchFromSupabase<TransactionEntity>("transactions")
            if (remote.isNotEmpty()) {
                transactionDao.insertAll(remote)
                synced++
            } else synced++
        } catch (e: Exception) {
            errors.add("transactions: ${e.message}")
        }

        // Pull tasks
        try {
            val remote = fetchFromSupabase<TaskEntity>("tasks")
            remote.forEach { taskDao.insert(it) }
            synced++
        } catch (e: Exception) {
            errors.add("tasks: ${e.message}")
        }

        // Pull events
        try {
            val remote = fetchFromSupabase<EventEntity>("events")
            remote.forEach { eventDao.insert(it) }
            synced++
        } catch (e: Exception) {
            errors.add("events: ${e.message}")
        }

        // Pull merchant categories
        try {
            val remote = fetchFromSupabase<MerchantCategoryEntity>("merchant_categories")
            remote.forEach { merchantCategoryDao.insert(it) }
            synced++
        } catch (e: Exception) {
            errors.add("merchant_categories: ${e.message}")
        }

        SyncResult(
            success = errors.isEmpty(),
            message = if (errors.isEmpty()) "Pulled $synced tables" else "Failed: ${errors.joinToString()}"
        )
    }

    private fun <T> upsertToSupabase(table: String, data: List<T>): Boolean {
        val json = gson.toJson(data)
        val request = Request.Builder()
            .url("${ApiConfig.SUPABASE_URL}/rest/v1/$table")
            .post(json.toRequestBody(jsonMediaType))
            .addHeader("apikey", ApiConfig.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer ${ApiConfig.SUPABASE_ANON_KEY}")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
            .build()

        return try {
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    private inline fun <reified T> fetchFromSupabase(table: String): List<T> {
        val request = Request.Builder()
            .url("${ApiConfig.SUPABASE_URL}/rest/v1/$table?select=*")
            .get()
            .addHeader("apikey", ApiConfig.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer ${ApiConfig.SUPABASE_ANON_KEY}")
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: "[]"
                val type = object : TypeToken<List<T>>() {}.type
                gson.fromJson(body, type) ?: emptyList()
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}

data class SyncResult(
    val success: Boolean,
    val message: String
)
