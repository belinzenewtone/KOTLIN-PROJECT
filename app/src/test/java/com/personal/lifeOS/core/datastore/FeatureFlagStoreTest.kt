package com.personal.lifeOS.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class FeatureFlagStoreTest {
    @Test
    fun `returns defaults when no value is persisted`() =
        runTest {
            val store = createStore(this)

            assertEquals(true, store.observe(FeatureFlag.OTA_UPDATES).first())
            assertEquals(true, store.observe(FeatureFlag.BACKGROUND_SYNC).first())
            assertEquals(true, store.isEnabled(FeatureFlag.ASSISTANT_ACTIONS))
        }

    @Test
    fun `applies remote values and ignores unknown keys`() =
        runTest {
            val store = createStore(this)

            store.applyRemoteValues(
                mapOf(
                    "ota_updates" to false,
                    "assistant_actions" to false,
                    "unknown_flag" to true,
                ),
            )

            assertEquals(false, store.isEnabled(FeatureFlag.OTA_UPDATES))
            assertEquals(false, store.isEnabled(FeatureFlag.ASSISTANT_ACTIONS))
            assertEquals(true, store.isEnabled(FeatureFlag.BACKGROUND_SYNC))
        }

    private fun createStore(scope: TestScope): FeatureFlagStore {
        val directory = Files.createTempDirectory("feature-flags").toFile()
        val backingStore =
            PreferenceDataStoreFactory.create(scope = scope.backgroundScope) {
                File(directory, "flags.preferences_pb")
            }
        return FeatureFlagStore(backingStore)
    }
}
