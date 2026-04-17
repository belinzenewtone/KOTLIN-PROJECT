package com.personal.lifeOS.core.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TaskReminderSchedulerTest {
    @Test
    fun `request code is stable for same user and task`() {
        val first = TaskReminderScheduler.reminderRequestCode("user-a", 444L)
        val second = TaskReminderScheduler.reminderRequestCode("user-a", 444L)

        assertEquals(first, second)
    }

    @Test
    fun `request code changes across users`() {
        val userA = TaskReminderScheduler.reminderRequestCode("user-a", 444L)
        val userB = TaskReminderScheduler.reminderRequestCode("user-b", 444L)

        assertNotEquals(userA, userB)
    }

    @Test
    fun `request code changes across reminder offsets`() {
        val first = TaskReminderScheduler.reminderRequestCode("user-a", 444L, offsetIndex = 0)
        val second = TaskReminderScheduler.reminderRequestCode("user-a", 444L, offsetIndex = 1)

        assertNotEquals(first, second)
    }

    @Test
    fun `compute reminder triggers uses configured offsets and skips past times`() {
        val deadline = 10_000_000L
        val now = deadline - 20L * 60L * 1000L

        val triggers = TaskReminderScheduler.computeReminderTriggers(deadline, listOf(30, 5), now)

        assertEquals(listOf(deadline - 5L * 60L * 1000L), triggers)
    }

    @Test
    fun `compute reminder triggers returns empty when no offsets configured`() {
        val deadline = 10_000_000L
        val now = deadline - 31L * 60L * 1000L

        val triggers = TaskReminderScheduler.computeReminderTriggers(deadline, emptyList(), now)

        assertEquals(emptyList<Long>(), triggers)
    }
}
