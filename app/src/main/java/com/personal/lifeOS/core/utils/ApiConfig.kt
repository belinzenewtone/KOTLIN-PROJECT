package com.personal.lifeOS.core.utils

import com.personal.lifeOS.BuildConfig

/**
 * API Configuration for cloud services.
 * Keys are stored in local.properties (which is gitignored) and injected via BuildConfig.
 */
object ApiConfig {
    // Supabase
    val SUPABASE_URL: String = BuildConfig.SUPABASE_URL
    val SUPABASE_ANON_KEY: String = BuildConfig.SUPABASE_ANON_KEY

    // Assistant proxy endpoint. Prefer explicit override from local.properties.
    private val ASSISTANT_PROXY_URL: String = BuildConfig.ASSISTANT_PROXY_URL

    fun isSupabaseConfigured(): Boolean {
        return SUPABASE_URL.isNotBlank() && SUPABASE_URL != "\"\"" && SUPABASE_ANON_KEY.isNotBlank()
    }

    fun assistantProxyEndpoint(): String {
        val explicit = ASSISTANT_PROXY_URL.trim()
        if (explicit.isNotBlank() && explicit != "\"\"") {
            return explicit
        }
        if (isSupabaseConfigured()) {
            return "${SUPABASE_URL}/functions/v1/assistant-proxy"
        }
        return ""
    }

    fun isAssistantProxyConfigured(): Boolean {
        return assistantProxyEndpoint().isNotBlank()
    }
}
