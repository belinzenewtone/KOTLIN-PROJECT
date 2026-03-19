package com.personal.lifeOS.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {
    @Test
    fun `hash is deterministic`() {
        val first = PasswordHasher.hash("secret-123")
        val second = PasswordHasher.hash("secret-123")
        assertEquals(first, second)
    }

    @Test
    fun `hash is not plaintext`() {
        val raw = "secret-123"
        val hashed = PasswordHasher.hash(raw)
        assertNotEquals(raw, hashed)
        assertEquals(64, hashed.length)
    }

    @Test
    fun `verify validates expected hash`() {
        val raw = "secret-123"
        val hashed = PasswordHasher.hash(raw)
        assertTrue(PasswordHasher.verify(raw, hashed))
        assertFalse(PasswordHasher.verify("wrong", hashed))
    }
}
