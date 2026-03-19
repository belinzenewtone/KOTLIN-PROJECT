package com.personal.lifeOS.core.sync

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.random.Random

@Singleton
class SyncBackoffPolicy
    @Inject
    constructor() {
        fun nextRetryAt(
            attempt: Int,
            now: Long = System.currentTimeMillis(),
        ): Long {
            val boundedAttempt = attempt.coerceAtLeast(1).coerceAtMost(7)
            val exponentialMs = (2.0.pow(boundedAttempt.toDouble()) * 1_000L).toLong()
            val jitter = Random.nextLong(0L, 900L)
            val capped = exponentialMs.coerceAtMost(15 * 60 * 1000L)
            return now + capped + jitter
        }
    }
