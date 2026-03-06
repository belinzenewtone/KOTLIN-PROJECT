package com.personal.lifeOS.features.assistant.data.datasource

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
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
 * OpenAI API client for intelligent assistant responses.
 * Falls back to local AI engine if API is not configured or fails.
 */
@Singleton
class OpenAIClient @Inject constructor() {

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Send a message to OpenAI with financial/productivity context.
     *
     * @param userMessage The user's question
     * @param context Aggregated data context (spending, tasks, events summary)
     * @return AI response text, or null if failed
     */
    suspend fun chat(userMessage: String, context: String): String? = withContext(Dispatchers.IO) {
        if (!ApiConfig.isOpenAIConfigured()) return@withContext null

        try {
            val request = ChatRequest(
                model = ApiConfig.OPENAI_MODEL,
                messages = listOf(
                    Message(
                        role = "system",
                        content = """You are BELTECH Assistant, a smart personal finance and productivity AI built into the BELTECH app.
                            |You help users understand their spending, tasks, and schedule.
                            |Be concise, friendly, and use KES (Kenyan Shillings) for all currency.
                            |Use bold (**text**) for emphasis. Use emoji sparingly.
                            |
                            |Here is the user's current data:
                            |$context
                        """.trimMargin()
                    ),
                    Message(role = "user", content = userMessage)
                ),
                maxTokens = 500,
                temperature = 0.7
            )

            val json = gson.toJson(request)
            val httpRequest = Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .post(json.toRequestBody(jsonMediaType))
                .addHeader("Authorization", "Bearer ${ApiConfig.OPENAI_API_KEY}")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(httpRequest).execute()

            if (response.isSuccessful) {
                val body = response.body?.string() ?: return@withContext null
                val chatResponse = gson.fromJson(body, ChatResponse::class.java)
                chatResponse.choices?.firstOrNull()?.message?.content
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Request/Response data classes
    data class ChatRequest(
        val model: String,
        val messages: List<Message>,
        @SerializedName("max_tokens") val maxTokens: Int,
        val temperature: Double
    )

    data class Message(
        val role: String,
        val content: String
    )

    data class ChatResponse(
        val choices: List<Choice>?
    )

    data class Choice(
        val message: Message?
    )
}
