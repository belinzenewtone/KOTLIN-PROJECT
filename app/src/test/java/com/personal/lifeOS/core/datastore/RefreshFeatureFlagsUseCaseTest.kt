package com.personal.lifeOS.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.personal.lifeOS.core.network.FeatureFlagRemoteDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class RefreshFeatureFlagsUseCaseTest {
    @Test
    fun `persists remote values on success`() =
        runTest {
            val store = createStore(this)
            val remote =
                object : FeatureFlagRemoteDataSource {
                    override suspend fun fetchFlags(): Result<Map<String, Boolean>> {
                        return Result.success(
                            mapOf(
                                "sms_import" to false,
                                "background_sync" to false,
                            ),
                        )
                    }
                }
            val useCase = RefreshFeatureFlagsUseCase(remote, store)

            val result = useCase()

            assertTrue(result.isSuccess)
            assertEquals(false, store.isEnabled(FeatureFlag.SMS_IMPORT))
            assertEquals(false, store.isEnabled(FeatureFlag.BACKGROUND_SYNC))
        }

    @Test
    fun `does not mutate store on remote failure`() =
        runTest {
            val store = createStore(this)
            val remote =
                object : FeatureFlagRemoteDataSource {
                    override suspend fun fetchFlags(): Result<Map<String, Boolean>> {
                        return Result.failure(IllegalStateException("network down"))
                    }
                }
            val useCase = RefreshFeatureFlagsUseCase(remote, store)

            val result = useCase()

            assertTrue(result.isFailure)
            assertEquals(true, store.isEnabled(FeatureFlag.OTA_UPDATES))
        }

    private fun createStore(scope: TestScope): FeatureFlagStore {
        val directory = Files.createTempDirectory("feature-flag-refresh").toFile()
        val backingStore =
            PreferenceDataStoreFactory.create(scope = scope.backgroundScope) {
                File(directory, "flags.preferences_pb")
            }
        return FeatureFlagStore(backingStore)
    }
}
