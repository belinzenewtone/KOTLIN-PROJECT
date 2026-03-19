package com.personal.lifeOS.core.database

import java.util.concurrent.atomic.AtomicLong

/**
 * Generates stable local ids for records that will also sync to cloud.
 */
object LocalIdGenerator {
    private val lastIssued = AtomicLong(System.currentTimeMillis() * 1000L)

    fun nextId(): Long {
        while (true) {
            val previous = lastIssued.get()
            val candidate = maxOf(previous + 1L, System.currentTimeMillis() * 1000L)
            if (lastIssued.compareAndSet(previous, candidate)) {
                return candidate
            }
        }
    }
}
