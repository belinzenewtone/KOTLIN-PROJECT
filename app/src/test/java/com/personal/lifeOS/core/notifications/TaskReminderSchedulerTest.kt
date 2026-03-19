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
}
