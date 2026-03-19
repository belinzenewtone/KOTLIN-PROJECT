package com.personal.lifeOS.core.utils

import androidx.room.withTransaction
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.personal.lifeOS.core.database.LifeOSDatabase
import com.personal.lifeOS.core.database.dao.BudgetDao
import com.personal.lifeOS.core.database.dao.EventDao
import com.personal.lifeOS.core.database.dao.IncomeDao
import com.personal.lifeOS.core.database.dao.MerchantCategoryDao
import com.personal.lifeOS.core.database.dao.RecurringRuleDao
import com.personal.lifeOS.core.database.dao.TaskDao
import com.personal.lifeOS.core.database.dao.TransactionDao
import com.personal.lifeOS.core.database.entity.BudgetEntity
import com.personal.lifeOS.core.database.entity.EventEntity
import com.personal.lifeOS.core.database.entity.IncomeEntity
import com.personal.lifeOS.core.database.entity.MerchantCategoryEntity
import com.personal.lifeOS.core.database.entity.RecurringRuleEntity
import com.personal.lifeOS.core.database.entity.TaskEntity
import com.personal.lifeOS.core.database.entity.TransactionEntity
import com.personal.lifeOS.core.security.AuthSessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Syncs local Room database with Supabase cloud using authenticated user sessions.
 */
@Singleton
class CloudSyncService
    @Inject
    constructor(
        private val database: LifeOSDatabase,
        private val transactionDao: TransactionDao,
        private val taskDao: TaskDao,
        private val eventDao: EventDao,
        private val merchantCategoryDao: MerchantCategoryDao,
        private val budgetDao: BudgetDao,
        private val incomeDao: IncomeDao,
        private val recurringRuleDao: RecurringRuleDao,
        private val authSessionStore: AuthSessionStore,
    ) {
        private val gson =
            GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create()
        private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

        private val client =
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        private val retryPolicy = SyncRetryPolicy()

        suspend fun pushToCloud(): SyncResult =
            withContext(Dispatchers.IO) {
                if (!ApiConfig.isSupabaseConfigured()) {
                    return@withContext SyncResult(success = false, message = "Supabase not configured")
                }

                val accessToken = authSessionStore.getAccessToken()
                val userId = authSessionStore.getUserId()
                if (accessToken.isBlank() || userId.isBlank()) {
                    return@withContext SyncResult(success = false, message = "Sign in required before cloud sync")
                }

                var synced = 0
                val errors = mutableListOf<String>()

                try {
                    val transactions = transactionDao.getAllForSync(userId).map { it.copy(userId = userId) }
                    if (transactions.isNotEmpty()) {
                        val success = upsertToSupabase("transactions", transactions, accessToken)
                        if (success) synced++ else errors.add("transactions")
                    } else {
                        synced++
                    }
                } catch (e: Exception) {
                    errors.add("transactions: ${e.message}")
                }

                try {
                    val tasks = taskDao.getAllForSync(userId).map { it.copy(userId = userId) }
                    if (tasks.isNotEmpty()) {
                        val success = upsertToSupabase("tasks", tasks, accessToken)
                        if (success) synced++ else errors.add("tasks")
                    } else {
                        synced++
                    }
                } catch (e: Exception) {
                    errors.add("tasks: ${e.message}")
                }

                try {
                    val events = eventDao.getAllForSync(userId).map { it.copy(userId = userId) }
                    if (events.isNotEmpty()) {
                        val success = upsertToSupabase("events", events, accessToken)
                        if (success) synced++ else errors.add("events")
                    } else {
                        synced++
                    }
                } catch (e: Exception) {
                    errors.add("events: ${e.message}")
                }

                try {
                    val merchants = merchantCategoryDao.getAllForSync(userId).map { it.copy(userId = userId) }
                    if (merchants.isNotEmpty()) {
                        val success = upsertToSupabase("merchant_categories", merchants, accessToken)
                        if (success) synced++ else errors.add("merchant_categories")
                    } else {
                        synced++
                    }
                } catch (e: Exception) {
                    errors.add("merchant_categories: ${e.message}")
                }

                try {
                    val budgets = budgetDao.getAllForSync(userId).map { it.copy(userId = userId) }
                    if (budgets.isNotEmpty()) {
                        val success = upsertToSupabase("budgets", budgets, accessToken)
                        if (success) synced++ else errors.add("budgets")
                    } else {
                        synced++
                    }
                } catch (e: Exception) {
                    errors.add("budgets: ${e.message}")
                }

                try {
                    val incomes = incomeDao.getAllForSync(userId).map { it.copy(userId = userId) }
                    if (incomes.isNotEmpty()) {
                        val success = upsertToSupabase("incomes", incomes, accessToken)
                        if (success) synced++ else errors.add("incomes")
                    } else {
                        synced++
                    }
                } catch (e: Exception) {
                    errors.add("incomes: ${e.message}")
                }

                try {
                    val recurringRules = recurringRuleDao.getAllForSync(userId).map { it.copy(userId = userId) }
                    if (recurringRules.isNotEmpty()) {
                        val success = upsertToSupabase("recurring_rules", recurringRules, accessToken)
                        if (success) synced++ else errors.add("recurring_rules")
                    } else {
                        synced++
                    }
                } catch (e: Exception) {
                    errors.add("recurring_rules: ${e.message}")
                }

                SyncResult(
                    success = errors.isEmpty(),
                    message = if (errors.isEmpty()) "Synced $synced tables" else "Failed: ${errors.joinToString()}",
                )
            }

        suspend fun pullFromCloud(): SyncResult =
            withContext(Dispatchers.IO) {
                if (!ApiConfig.isSupabaseConfigured()) {
                    return@withContext SyncResult(success = false, message = "Supabase not configured")
                }

                val accessToken = authSessionStore.getAccessToken()
                val userId = authSessionStore.getUserId()
                if (accessToken.isBlank() || userId.isBlank()) {
                    return@withContext SyncResult(success = false, message = "Sign in required before cloud sync")
                }

                var synced = 0
                val errors = mutableListOf<String>()

                try {
                    val remote =
                        fetchFromSupabase<TransactionEntity>("transactions", accessToken, userId)
                            .map { it.copy(userId = userId) }
                    database.withTransaction {
                        transactionDao.deleteByUserId(userId)
                        if (remote.isNotEmpty()) transactionDao.insertAll(remote)
                    }
                    synced++
                } catch (e: Exception) {
                    errors.add("transactions: ${e.message}")
                }

                try {
                    val remote =
                        fetchFromSupabase<TaskEntity>("tasks", accessToken, userId)
                            .map { it.copy(userId = userId) }
                    database.withTransaction {
                        taskDao.deleteByUserId(userId)
                        remote.forEach { taskDao.insert(it) }
                    }
                    synced++
                } catch (e: Exception) {
                    errors.add("tasks: ${e.message}")
                }

                try {
                    val remote =
                        fetchFromSupabase<EventEntity>("events", accessToken, userId)
                            .map { it.copy(userId = userId) }
                    database.withTransaction {
                        eventDao.deleteByUserId(userId)
                        remote.forEach { eventDao.insert(it) }
                    }
                    synced++
                } catch (e: Exception) {
                    errors.add("events: ${e.message}")
                }

                try {
                    val remote =
                        fetchFromSupabase<MerchantCategoryEntity>("merchant_categories", accessToken, userId)
                            .map { it.copy(userId = userId) }
                    database.withTransaction {
                        merchantCategoryDao.deleteByUserId(userId)
                        remote.forEach { merchantCategoryDao.insert(it) }
                    }
                    synced++
                } catch (e: Exception) {
                    errors.add("merchant_categories: ${e.message}")
                }

                try {
                    val remote =
                        fetchFromSupabase<BudgetEntity>("budgets", accessToken, userId)
                            .map { it.copy(userId = userId) }
                    database.withTransaction {
                        budgetDao.deleteByUserId(userId)
                        if (remote.isNotEmpty()) budgetDao.insertAll(remote)
                    }
                    synced++
                } catch (e: Exception) {
                    errors.add("budgets: ${e.message}")
                }

                try {
                    val remote =
                        fetchFromSupabase<IncomeEntity>("incomes", accessToken, userId)
                            .map { it.copy(userId = userId) }
                    database.withTransaction {
                        incomeDao.deleteByUserId(userId)
                        if (remote.isNotEmpty()) incomeDao.insertAll(remote)
                    }
                    synced++
                } catch (e: Exception) {
                    errors.add("incomes: ${e.message}")
                }

                try {
                    val remote =
                        fetchFromSupabase<RecurringRuleEntity>("recurring_rules", accessToken, userId)
                            .map { it.copy(userId = userId) }
                    database.withTransaction {
                        recurringRuleDao.deleteByUserId(userId)
                        if (remote.isNotEmpty()) recurringRuleDao.insertAll(remote)
                    }
                    synced++
                } catch (e: Exception) {
                    errors.add("recurring_rules: ${e.message}")
                }

                SyncResult(
                    success = errors.isEmpty(),
                    message = if (errors.isEmpty()) "Pulled $synced tables" else "Failed: ${errors.joinToString()}",
                )
            }

        private suspend fun <T> upsertToSupabase(
            table: String,
            data: List<T>,
            accessToken: String,
        ): Boolean {
            val payload = gson.toJson(data)
            val conflictTarget =
                when (table) {
                    "merchant_categories" -> "user_id,merchant"
                    else -> "user_id,id"
                }
            val url = "${ApiConfig.SUPABASE_URL}/rest/v1/$table?on_conflict=$conflictTarget"
            val result =
                executeWithRetry("push:$table") {
                    executeUpsertRequest(url = url, payload = payload, accessToken = accessToken)
                }

            if (result.isSuccessful) return true

            throw IllegalStateException(
                "Supabase upsert failed for $table (${result.code ?: 0}) ${result.body.take(200)}",
            )
        }

        private fun executeUpsertRequest(
            url: String,
            payload: String,
            accessToken: String,
        ): HttpCallResult {
            val request =
                Request.Builder()
                    .url(url)
                    .post(payload.toRequestBody(jsonMediaType))
                    .addHeader("apikey", ApiConfig.SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                    .build()

            return try {
                client.newCall(request).execute().use { response ->
                    HttpCallResult(
                        isSuccessful = response.isSuccessful,
                        code = response.code,
                        body = response.body?.string().orEmpty(),
                    )
                }
            } catch (e: Exception) {
                HttpCallResult(
                    isSuccessful = false,
                    throwable = e,
                )
            }
        }

        private suspend inline fun <reified T> fetchFromSupabase(
            table: String,
            accessToken: String,
            userId: String,
        ): List<T> {
            val result =
                executeWithRetry("pull:$table") {
                    val request =
                        Request.Builder()
                            .url("${ApiConfig.SUPABASE_URL}/rest/v1/$table?select=*&user_id=eq.$userId")
                            .get()
                            .addHeader("apikey", ApiConfig.SUPABASE_ANON_KEY)
                            .addHeader("Authorization", "Bearer $accessToken")
                            .build()

                    try {
                        client.newCall(request).execute().use { response ->
                            HttpCallResult(
                                isSuccessful = response.isSuccessful,
                                code = response.code,
                                body = response.body?.string().orEmpty(),
                            )
                        }
                    } catch (e: Exception) {
                        HttpCallResult(
                            isSuccessful = false,
                            throwable = e,
                        )
                    }
                }

            if (!result.isSuccessful) {
                throw IllegalStateException(
                    "Supabase pull failed for $table (${result.code ?: 0}) ${result.body.take(200)}",
                )
            }

            val body = if (result.body.isBlank()) "[]" else result.body
            val type = object : TypeToken<List<T>>() {}.type
            return gson.fromJson(body, type) ?: emptyList()
        }

        private suspend fun executeWithRetry(
            operation: String,
            requestBlock: () -> HttpCallResult,
        ): HttpCallResult {
            var attempt = 1
            var result = requestBlock()

            while (retryPolicy.shouldRetry(attempt, result.code, result.throwable)) {
                delay(retryPolicy.backoffDelayMs(attempt))
                attempt++
                result = requestBlock()
            }
            if (!result.isSuccessful && result.throwable != null) {
                throw IllegalStateException("$operation failed: ${result.throwable.message}", result.throwable)
            }
            return result
        }
    }

private data class HttpCallResult(
    val isSuccessful: Boolean,
    val code: Int? = null,
    val body: String = "",
    val throwable: Throwable? = null,
)

data class SyncResult(
    val success: Boolean,
    val message: String,
)
