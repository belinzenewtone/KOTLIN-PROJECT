package com.personal.lifeOS.core.utils

import kotlin.math.pow

/**
 * Retry policy for transient sync failures.
 */
class SyncRetryPolicy(
    private val maxAttempts: Int = 3,
    private val initialDelayMs: Long = 400L,
    private val maxDelayMs: Long = 3_000L,
) {
    fun shouldRetry(
        attempt: Int,
        httpCode: Int?,
        throwable: Throwable?,
    ): Boolean {
        if (attempt >= maxAttempts) return false
        if (throwable != null) return true
        if (httpCode == null) return false
        return httpCode in RETRYABLE_HTTP_CODES
    }

    fun backoffDelayMs(attempt: Int): Long {
        if (attempt <= 1) return initialDelayMs
        val delay = initialDelayMs * 2.0.pow((attempt - 1).toDouble())
        return delay.toLong().coerceAtMost(maxDelayMs)
    }

    companion object {
        val RETRYABLE_HTTP_CODES = setOf(408, 425, 429, 500, 502, 503, 504)
    }
}
