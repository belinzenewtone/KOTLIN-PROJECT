package com.personal.lifeOS.core.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SyncRetryPolicyTest {
    private val policy =
        SyncRetryPolicy(
            maxAttempts = 3,
            initialDelayMs = 100L,
            maxDelayMs = 500L,
        )

    @Test
    fun `retries for transient http status codes`() {
        assertTrue(policy.shouldRetry(attempt = 1, httpCode = 429, throwable = null))
        assertTrue(policy.shouldRetry(attempt = 1, httpCode = 503, throwable = null))
        assertFalse(policy.shouldRetry(attempt = 1, httpCode = 400, throwable = null))
    }

    @Test
    fun `retries for thrown network exception`() {
        assertTrue(policy.shouldRetry(attempt = 1, httpCode = null, throwable = IOException("timeout")))
    }

    @Test
    fun `stops retrying after max attempts`() {
        assertFalse(policy.shouldRetry(attempt = 3, httpCode = 503, throwable = null))
        assertFalse(policy.shouldRetry(attempt = 4, httpCode = null, throwable = IOException("network")))
    }

    @Test
    fun `uses exponential backoff capped by max delay`() {
        assertEquals(100L, policy.backoffDelayMs(1))
        assertEquals(200L, policy.backoffDelayMs(2))
        assertEquals(400L, policy.backoffDelayMs(3))
        assertEquals(500L, policy.backoffDelayMs(4))
    }
}
