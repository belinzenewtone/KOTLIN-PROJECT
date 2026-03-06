package com.personal.lifeOS.core.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class SupabaseClient(
    private val supabaseUrl: String = ApiConfig.SUPABASE_URL,
    private val supabaseKey: String = ApiConfig.SUPABASE_ANON_KEY
) {
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun baseHeaders(): Map<String, String> = mapOf(
        "apikey" to supabaseKey,
        "Authorization" to "Bearer $supabaseKey",
        "Content-Type" to "application/json",
        "Prefer" to "return=representation"
    )

    suspend fun <T> upsert(table: String, data: List<T>): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(data)
            val requestBuilder = Request.Builder()
                .url("$supabaseUrl/rest/v1/$table")
                .post(json.toRequestBody(jsonMediaType))

            baseHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }
            requestBuilder.addHeader("Prefer", "resolution=merge-duplicates,return=representation")

            val response = client.newCall(requestBuilder.build()).execute()
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun <T> fetchAll(table: String, type: Class<Array<T>>): List<T> = withContext(Dispatchers.IO) {
        try {
            val requestBuilder = Request.Builder()
                .url("$supabaseUrl/rest/v1/$table?select=*")
                .get()

            baseHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }

            val response = client.newCall(requestBuilder.build()).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: "[]"
                val array = gson.fromJson(body, type)
                array?.toList() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun delete(table: String, filter: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val requestBuilder = Request.Builder()
                .url("$supabaseUrl/rest/v1/$table?$filter")
                .delete()

            baseHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }

            val response = client.newCall(requestBuilder.build()).execute()
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
