package com.personal.lifeOS.features.auth.data

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

@Singleton
class SupabaseAuthClient
    @Inject
    constructor() {
        private val gson = Gson()
        private val json = "application/json; charset=utf-8".toMediaType()

        private val client =
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

        private val authUrl get() = "${ApiConfig.SUPABASE_URL}/auth/v1"

        private fun missingConfigError(): AuthResult.Error {
            return AuthResult.Error(
                "Backend is not configured in this build. Set SUPABASE_URL and SUPABASE_ANON_KEY, then rebuild.",
            )
        }

        suspend fun signUp(
            email: String,
            password: String,
            username: String,
        ): AuthResult =
            withContext(Dispatchers.IO) {
                if (!ApiConfig.isSupabaseConfigured()) {
                    return@withContext missingConfigError()
                }
                try {
                    val body =
                        gson.toJson(
                            mapOf(
                                "email" to email,
                                "password" to password,
                                "data" to mapOf("username" to username),
                            ),
                        )

                    val request =
                        Request.Builder()
                            .url("$authUrl/signup")
                            .post(body.toRequestBody(json))
                            .addHeader("apikey", ApiConfig.SUPABASE_ANON_KEY)
                            .addHeader("Content-Type", "application/json")
                            .build()

                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string() ?: ""

                    if (response.isSuccessful) {
                        val authResponse = gson.fromJson(responseBody, SupabaseAuthResponse::class.java)
                        AuthResult.Success(
                            userId = authResponse.user?.id ?: "",
                            email = authResponse.user?.email ?: email,
                            accessToken = authResponse.accessToken ?: "",
                            emailConfirmed = authResponse.user?.emailConfirmedAt != null,
                            username = username,
                            createdAt = authResponse.user?.createdAt ?: "",
                        )
                    } else {
                        val error =
                            try {
                                gson.fromJson(responseBody, SupabaseErrorResponse::class.java)
                            } catch (e: Exception) {
                                null
                            }
                        AuthResult.Error(error?.message ?: error?.msg ?: "Sign up failed (${response.code})")
                    }
                } catch (e: Exception) {
                    AuthResult.Error(e.message ?: "Network error")
                }
            }

        suspend fun signIn(
            email: String,
            password: String,
        ): AuthResult =
            withContext(Dispatchers.IO) {
                if (!ApiConfig.isSupabaseConfigured()) {
                    return@withContext missingConfigError()
                }
                try {
                    val body = gson.toJson(mapOf("email" to email, "password" to password))

                    val request =
                        Request.Builder()
                            .url("$authUrl/token?grant_type=password")
                            .post(body.toRequestBody(json))
                            .addHeader("apikey", ApiConfig.SUPABASE_ANON_KEY)
                            .addHeader("Content-Type", "application/json")
                            .build()

                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string() ?: ""

                    if (response.isSuccessful) {
                        val authResponse = gson.fromJson(responseBody, SupabaseAuthResponse::class.java)
                        val username = (authResponse.user?.userMetadata as? Map<*, *>)?.get("username")?.toString() ?: ""
                        AuthResult.Success(
                            userId = authResponse.user?.id ?: "",
                            email = authResponse.user?.email ?: email,
                            accessToken = authResponse.accessToken ?: "",
                            emailConfirmed = authResponse.user?.emailConfirmedAt != null,
                            username = username,
                            createdAt = authResponse.user?.createdAt ?: "",
                        )
                    } else {
                        val error =
                            try {
                                gson.fromJson(responseBody, SupabaseErrorResponse::class.java)
                            } catch (e: Exception) {
                                null
                            }
                        AuthResult.Error(error?.message ?: error?.msg ?: "Invalid email or password")
                    }
                } catch (e: Exception) {
                    AuthResult.Error(e.message ?: "Network error")
                }
            }

        suspend fun getUser(accessToken: String): AuthResult =
            withContext(Dispatchers.IO) {
                if (!ApiConfig.isSupabaseConfigured()) {
                    return@withContext missingConfigError()
                }
                try {
                    val request =
                        Request.Builder()
                            .url("$authUrl/user")
                            .get()
                            .addHeader("apikey", ApiConfig.SUPABASE_ANON_KEY)
                            .addHeader("Authorization", "Bearer $accessToken")
                            .build()

                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string() ?: ""

                    if (response.isSuccessful) {
                        val user = gson.fromJson(responseBody, SupabaseUser::class.java)
                        val username = (user.userMetadata as? Map<*, *>)?.get("username")?.toString() ?: ""
                        AuthResult.Success(
                            userId = user.id ?: "",
                            email = user.email ?: "",
                            accessToken = accessToken,
                            emailConfirmed = user.emailConfirmedAt != null,
                            username = username,
                            createdAt = user.createdAt ?: "",
                        )
                    } else {
                        // HTTP error (e.g. 401 Unauthorized) — token genuinely expired/revoked
                        AuthResult.Error("Session expired", isNetworkError = false)
                    }
                } catch (e: Exception) {
                    // Network-level failure — device is offline or server unreachable
                    AuthResult.Error(e.message ?: "Network error", isNetworkError = true)
                }
            }

        suspend fun resendVerification(email: String): Boolean =
            withContext(Dispatchers.IO) {
                if (!ApiConfig.isSupabaseConfigured()) {
                    return@withContext false
                }
                try {
                    val body = gson.toJson(mapOf("email" to email))
                    val request =
                        Request.Builder()
                            .url("$authUrl/resend")
                            .post(body.toRequestBody(json))
                            .addHeader("apikey", ApiConfig.SUPABASE_ANON_KEY)
                            .addHeader("Content-Type", "application/json")
                            .build()

                    client.newCall(request).execute().isSuccessful
                } catch (e: Exception) {
                    false
                }
            }

        suspend fun sendPasswordReset(email: String): Boolean =
            withContext(Dispatchers.IO) {
                if (!ApiConfig.isSupabaseConfigured()) {
                    return@withContext false
                }
                try {
                    val body = gson.toJson(mapOf("email" to email))
                    val request =
                        Request.Builder()
                            .url("$authUrl/recover")
                            .post(body.toRequestBody(json))
                            .addHeader("apikey", ApiConfig.SUPABASE_ANON_KEY)
                            .addHeader("Content-Type", "application/json")
                            .build()

                    client.newCall(request).execute().isSuccessful
                } catch (e: Exception) {
                    false
                }
            }

        // Data classes
        data class SupabaseAuthResponse(
            @SerializedName("access_token") val accessToken: String?,
            @SerializedName("refresh_token") val refreshToken: String?,
            val user: SupabaseUser?,
        )

        data class SupabaseUser(
            val id: String?,
            val email: String?,
            @SerializedName("email_confirmed_at") val emailConfirmedAt: String?,
            @SerializedName("created_at") val createdAt: String?,
            @SerializedName("user_metadata") val userMetadata: Any?,
        )

        data class SupabaseErrorResponse(
            val message: String?,
            val msg: String?,
            @SerializedName("error_description") val errorDescription: String?,
        )
    }

sealed class AuthResult {
    data class Success(
        val userId: String,
        val email: String,
        val accessToken: String,
        val emailConfirmed: Boolean,
        val username: String,
        val createdAt: String,
    ) : AuthResult()

    /**
     * @param message   Human-readable error description.
     * @param isNetworkError  True when the failure was caused by a connectivity problem
     *   (no internet, timeout, socket exception) rather than an actual auth rejection
     *   (e.g. HTTP 401 / token expired).  The distinction is used in
     *   [AuthViewModel.checkExistingSession] to decide whether to trust the locally
     *   cached session while offline or to require a fresh sign-in.
     */
    data class Error(val message: String, val isNetworkError: Boolean = false) : AuthResult()
}
