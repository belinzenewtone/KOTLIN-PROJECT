package com.personal.lifeOS.core.network

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.personal.lifeOS.core.security.AuthSessionStore
import com.personal.lifeOS.core.utils.ApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

interface FeatureFlagRemoteDataSource {
    suspend fun fetchFlags(): Result<Map<String, Boolean>>
}

private data class RemoteFeatureFlagDto(
    val key: String,
    val enabled: Boolean,
)

@Singleton
class SupabaseFeatureFlagRemoteDataSource
    @Inject
    constructor(
        private val authSessionStore: AuthSessionStore,
    ) : FeatureFlagRemoteDataSource {
        private val client by lazy { OkHttpClient() }
        private val gson by lazy { Gson() }

        override suspend fun fetchFlags(): Result<Map<String, Boolean>> =
            withContext(Dispatchers.IO) {
                runCatching {
                    if (!ApiConfig.isSupabaseConfigured()) {
                        return@runCatching emptyMap()
                    }

                    val accessToken = authSessionStore.getAccessToken()
                    if (accessToken.isBlank()) {
                        return@runCatching emptyMap()
                    }

                    val request =
                        Request.Builder()
                            .url("${ApiConfig.SUPABASE_URL}/rest/v1/feature_flags?select=key,enabled")
                            .get()
                            .addHeader("apikey", ApiConfig.SUPABASE_ANON_KEY)
                            .addHeader("Authorization", "Bearer $accessToken")
                            .addHeader("Accept", "application/json")
                            .build()

                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            error("Feature flag fetch failed (${response.code}): ${response.body?.string().orEmpty()}")
                        }

                        val json = response.body?.string().orEmpty()
                        if (json.isBlank()) {
                            return@use emptyMap()
                        }

                        val listType = object : TypeToken<List<RemoteFeatureFlagDto>>() {}.type
                        val parsed: List<RemoteFeatureFlagDto> = gson.fromJson(json, listType) ?: emptyList()
                        parsed.associate { dto -> dto.key to dto.enabled }
                    }
                }
            }
    }
