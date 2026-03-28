package com.personal.lifeOS.core.sync

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.personal.lifeOS.core.datastore.FeatureFlagStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SyncCircuitBreakerTest {
    @Test
    fun `breaker opens after repeated failures and closes on success`() =
        runTest {
            val datastoreFile = File(createTempDir(), "sync-breaker.preferences_pb")
            val dataStore =
                PreferenceDataStoreFactory.create(
                    scope = backgroundScope,
                    produceFile = { datastoreFile },
                )
            val breaker =
                SyncCircuitBreaker(
                    dataStore = dataStore,
                    featureFlagStore = FeatureFlagStore(dataStore),
                )
            val now = 1_700_000_000_000L

            breaker.onFailure(now)
            breaker.onFailure(now + 1_000L)
            breaker.onFailure(now + 2_000L)

            assertFalse(breaker.shouldAllow(now + 3_000L))

            breaker.onSuccess()

            assertTrue(breaker.shouldAllow(now + 4_000L))
        }
}
