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

    // OpenAI
    val OPENAI_API_KEY: String = BuildConfig.OPENAI_API_KEY
    const val OPENAI_MODEL = "gpt-4o-mini"

    fun isSupabaseConfigured(): Boolean {
        return SUPABASE_URL.isNotBlank() && SUPABASE_URL != "\"\"" && SUPABASE_ANON_KEY.isNotBlank()
    }

    fun isOpenAIConfigured(): Boolean {
        return OPENAI_API_KEY.isNotBlank() && OPENAI_API_KEY != "\"\""
    }
}
