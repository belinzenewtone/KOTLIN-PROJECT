package com.personal.lifeOS.core.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.personal.lifeOS.core.datastore.FeatureFlag
import com.personal.lifeOS.core.datastore.FeatureFlagStore
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

data class SyncCircuitBreakerState(
    val consecutiveFailures: Int = 0,
    val openUntilMillis: Long = 0L,
    val lastFailureAtMillis: Long = 0L,
) {
    fun isOpen(nowMillis: Long): Boolean = openUntilMillis > nowMillis
}

@Singleton
class SyncCircuitBreaker
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
        private val featureFlagStore: FeatureFlagStore,
    ) {
        suspend fun snapshot(): SyncCircuitBreakerState {
            val prefs = dataStore.data.first()
            return SyncCircuitBreakerState(
                consecutiveFailures = prefs[KEY_FAILURES] ?: 0,
                openUntilMillis = prefs[KEY_OPEN_UNTIL] ?: 0L,
                lastFailureAtMillis = prefs[KEY_LAST_FAILURE_AT] ?: 0L,
            )
        }

        suspend fun shouldAllow(nowMillis: Long = System.currentTimeMillis()): Boolean {
            if (!featureFlagStore.isEnabled(FeatureFlag.SYNC_CIRCUIT_BREAKER)) return true
            return !snapshot().isOpen(nowMillis)
        }

        suspend fun onSuccess() {
            dataStore.edit { prefs ->
                prefs[KEY_FAILURES] = 0
                prefs[KEY_OPEN_UNTIL] = 0L
            }
        }

        suspend fun onFailure(nowMillis: Long = System.currentTimeMillis()): SyncCircuitBreakerState {
            if (!featureFlagStore.isEnabled(FeatureFlag.SYNC_CIRCUIT_BREAKER)) {
                return snapshot()
            }
            val current = snapshot()
            val failures = current.consecutiveFailures + 1
            val shouldOpen = failures >= MIN_FAILURES_TO_OPEN
            val openForMillis =
                if (shouldOpen) {
                    BASE_OPEN_DURATION_MS * min(1 shl (failures - MIN_FAILURES_TO_OPEN), 8)
                } else {
                    0L
                }
            val nextState =
                SyncCircuitBreakerState(
                    consecutiveFailures = failures,
                    openUntilMillis = if (shouldOpen) nowMillis + openForMillis else 0L,
                    lastFailureAtMillis = nowMillis,
                )
            dataStore.edit { prefs ->
                prefs[KEY_FAILURES] = nextState.consecutiveFailures
                prefs[KEY_OPEN_UNTIL] = nextState.openUntilMillis
                prefs[KEY_LAST_FAILURE_AT] = nextState.lastFailureAtMillis
            }
            return nextState
        }

        companion object {
            private const val MIN_FAILURES_TO_OPEN = 3
            private const val BASE_OPEN_DURATION_MS = 5 * 60 * 1000L
            private val KEY_FAILURES = intPreferencesKey("sync_circuit_breaker_failures")
            private val KEY_OPEN_UNTIL = longPreferencesKey("sync_circuit_breaker_open_until")
            private val KEY_LAST_FAILURE_AT = longPreferencesKey("sync_circuit_breaker_last_failure_at")
        }
    }
