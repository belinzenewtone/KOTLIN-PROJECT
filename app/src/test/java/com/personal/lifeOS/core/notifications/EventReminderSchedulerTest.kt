package com.personal.lifeOS.core.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class EventReminderSchedulerTest {
    @Test
    fun `request code is stable for same user and event`() {
        val first = EventReminderScheduler.reminderRequestCode("user-a", 123L)
        val second = EventReminderScheduler.reminderRequestCode("user-a", 123L)

        assertEquals(first, second)
    }

    @Test
    fun `request code changes across users`() {
        val userA = EventReminderScheduler.reminderRequestCode("user-a", 123L)
        val userB = EventReminderScheduler.reminderRequestCode("user-b", 123L)

        assertNotEquals(userA, userB)
    }
}
