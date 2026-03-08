package com.personal.lifeOS.features.assistant.data.datasource

import com.google.gson.Gson
import com.personal.lifeOS.core.utils.ApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Assistant client that calls a backend proxy endpoint.
 * No model provider API keys are stored in the Android app.
 */
@Singleton
class OpenAIClient
    @Inject
    constructor() {
        private val gson = Gson()
        private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

        private val client =
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()

        /**
         * Send a message to the assistant proxy with financial/productivity context.
         *
         * @param userMessage The user's question
         * @param context Aggregated data context (spending, tasks, events summary)
         * @return AI response text, or null if failed
         */
        suspend fun chat(
            userMessage: String,
            context: String,
        ): String? =
            withContext(Dispatchers.IO) {
                val endpoint = ApiConfig.assistantProxyEndpoint()
                if (endpoint.isBlank()) return@withContext null

                try {
                    val json =
                        gson.toJson(
                            mapOf(
                                "prompt" to userMessage,
                                "context" to context,
                            ),
                        )
                    val httpRequest =
                        Request.Builder()
                            .url(endpoint)
                            .post(json.toRequestBody(jsonMediaType))
                            .addHeader("Content-Type", "application/json")
                            .build()

                    val response = client.newCall(httpRequest).execute()

                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: return@withContext null
                        val payload = gson.fromJson(body, Map::class.java)
                        val reply = payload["reply"]?.toString()?.trim()
                        if (reply.isNullOrBlank()) null else reply
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
    }
