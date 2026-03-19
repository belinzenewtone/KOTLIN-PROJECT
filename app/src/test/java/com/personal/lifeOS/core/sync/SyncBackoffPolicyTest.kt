package com.personal.lifeOS.core.sync

import org.junit.Assert.assertTrue
import org.junit.Test

class SyncBackoffPolicyTest {
    @Test
    fun `next retry increases with attempts and stays capped`() {
        val policy = SyncBackoffPolicy()
        val now = 1_000_000L

        val retry1 = policy.nextRetryAt(attempt = 1, now = now)
        val retry2 = policy.nextRetryAt(attempt = 2, now = now)
        val retry7 = policy.nextRetryAt(attempt = 7, now = now)
        val retry20 = policy.nextRetryAt(attempt = 20, now = now)

        assertTrue(retry2 > retry1)
        assertTrue(retry7 > retry2)
        assertTrue(retry20 - now <= 15 * 60 * 1000L + 900L)
    }
}
