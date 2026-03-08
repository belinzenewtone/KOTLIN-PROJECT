package com.personal.lifeOS.core.security

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AuthSessionStoreTest {
    @Test
    fun `session persists across store instances and can be cleared`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        val first = AuthSessionStore(context)
        first.saveSession("token-123", "user-abc")

        val second = AuthSessionStore(context)
        assertEquals("token-123", second.getAccessToken())
        assertEquals("user-abc", second.getUserId())

        second.clearSession()

        val third = AuthSessionStore(context)
        assertEquals("", third.getAccessToken())
        assertEquals("", third.getUserId())
    }
}
