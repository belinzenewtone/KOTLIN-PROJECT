package com.personal.lifeOS.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "lifeos_preferences")

object AppSettingsKeys {
    val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    val ONBOARDING_STEP = intPreferencesKey("onboarding_step")
    val ONBOARDING_PRIMARY_GOAL = stringPreferencesKey("onboarding_primary_goal")
    // Runtime permission ask-tracking — set true after we show the in-app rationale once.
    // We never nag the user; if they dismiss, we respect that forever.
    val NOTIFICATION_PERMISSION_ASKED = booleanPreferencesKey("notification_permission_asked")
    val SMS_PERMISSION_ASKED = booleanPreferencesKey("sms_permission_asked")
    val FULIZA_LIMIT_KES = longPreferencesKey("fuliza_limit_kes")
    /**
     * Last known Fuliza outstanding balance derived from SMS.
     * Updated by FULIZA_CHARGE (direct outstanding) or LOAN repayment (limit - available).
     * Stored as Long (rounded KES, e.g. 508 for Ksh 508.16 → multiply by 100 for cents).
     * We store as centesimal (×100) to preserve decimal precision without using Double in DataStore.
     */
    val FULIZA_OUTSTANDING_KES_CENTS = longPreferencesKey("fuliza_outstanding_kes_cents")
}

@Singleton
@Suppress("TooManyFunctions")
class AppSettingsStore
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) {
        private val profileNameKey = stringPreferencesKey("user_name")
        private val profileMemberSinceKey = longPreferencesKey("member_since")

        fun biometricEnabledFlow(): Flow<Boolean> {
            return dataStore.data.map { prefs ->
                prefs[AppSettingsKeys.BIOMETRIC_ENABLED] ?: false
            }
        }

        suspend fun isBiometricEnabled(): Boolean {
            return dataStore.data.first()[AppSettingsKeys.BIOMETRIC_ENABLED] ?: false
        }

        suspend fun setBiometricEnabled(enabled: Boolean) {
            dataStore.edit { prefs ->
                prefs[AppSettingsKeys.BIOMETRIC_ENABLED] = enabled
            }
        }

        fun notificationsEnabledFlow(): Flow<Boolean> {
            return dataStore.data.map { prefs ->
                prefs[AppSettingsKeys.NOTIFICATIONS_ENABLED] ?: true
            }
        }

        suspend fun areNotificationsEnabled(): Boolean {
            return dataStore.data.first()[AppSettingsKeys.NOTIFICATIONS_ENABLED] ?: true
        }

        suspend fun setNotificationsEnabled(enabled: Boolean) {
            dataStore.edit { prefs ->
                prefs[AppSettingsKeys.NOTIFICATIONS_ENABLED] = enabled
            }
        }

        fun onboardingCompletedFlow(): Flow<Boolean> {
            return dataStore.data.map { prefs ->
                prefs[AppSettingsKeys.ONBOARDING_COMPLETED] ?: !prefs[profileNameKey].isNullOrBlank()
            }
        }

        suspend fun getProfileName(): String {
            return dataStore.data.first()[profileNameKey].orEmpty()
        }

        suspend fun isOnboardingCompleted(): Boolean {
            val prefs = dataStore.data.first()
            return prefs[AppSettingsKeys.ONBOARDING_COMPLETED] ?: !prefs[profileNameKey].isNullOrBlank()
        }

        fun onboardingStepFlow(): Flow<Int> {
            return dataStore.data.map { prefs ->
                prefs[AppSettingsKeys.ONBOARDING_STEP] ?: 1
            }
        }

        suspend fun getOnboardingStep(): Int {
            return dataStore.data.first()[AppSettingsKeys.ONBOARDING_STEP] ?: 1
        }

        fun onboardingPrimaryGoalFlow(): Flow<String> {
            return dataStore.data.map { prefs ->
                prefs[AppSettingsKeys.ONBOARDING_PRIMARY_GOAL].orEmpty()
            }
        }

        suspend fun getOnboardingPrimaryGoal(): String {
            return dataStore.data.first()[AppSettingsKeys.ONBOARDING_PRIMARY_GOAL].orEmpty()
        }

        suspend fun setOnboardingStep(step: Int) {
            dataStore.edit { prefs ->
                prefs[AppSettingsKeys.ONBOARDING_STEP] = step.coerceIn(1, 4)
            }
        }

        suspend fun setOnboardingPrimaryGoal(goal: String) {
            dataStore.edit { prefs ->
                prefs[AppSettingsKeys.ONBOARDING_PRIMARY_GOAL] = goal
            }
        }

        suspend fun completeOnboarding(
            fullName: String,
            primaryGoal: String,
        ) {
            val trimmedName = fullName.trim()
            dataStore.edit { prefs ->
                prefs[AppSettingsKeys.ONBOARDING_COMPLETED] = true
                prefs[AppSettingsKeys.ONBOARDING_STEP] = 4
                prefs[AppSettingsKeys.ONBOARDING_PRIMARY_GOAL] = primaryGoal
                if (trimmedName.isNotEmpty()) {
                    prefs[profileNameKey] = trimmedName
                    if ((prefs[profileMemberSinceKey] ?: 0L) == 0L) {
                        prefs[profileMemberSinceKey] = System.currentTimeMillis()
                    }
                }
            }
        }

        suspend fun resetOnboarding() {
            dataStore.edit { prefs ->
                prefs[AppSettingsKeys.ONBOARDING_COMPLETED] = false
                prefs[AppSettingsKeys.ONBOARDING_STEP] = 1
                prefs[AppSettingsKeys.ONBOARDING_PRIMARY_GOAL] = ""
            }
        }

        // ── Permission ask-tracking ──────────────────────────────────────────

        suspend fun wasNotificationPermissionAsked(): Boolean =
            dataStore.data.first()[AppSettingsKeys.NOTIFICATION_PERMISSION_ASKED] ?: false

        suspend fun markNotificationPermissionAsked() {
            dataStore.edit { prefs ->
                prefs[AppSettingsKeys.NOTIFICATION_PERMISSION_ASKED] = true
            }
        }

        suspend fun wasSmsPermissionAsked(): Boolean =
            dataStore.data.first()[AppSettingsKeys.SMS_PERMISSION_ASKED] ?: false

        suspend fun markSmsPermissionAsked() {
            dataStore.edit { prefs ->
                prefs[AppSettingsKeys.SMS_PERMISSION_ASKED] = true
            }
        }

        fun fulizaLimitKesFlow(): Flow<Double?> {
            return dataStore.data.map { prefs ->
                prefs[AppSettingsKeys.FULIZA_LIMIT_KES]?.toDouble()
            }
        }

        suspend fun getFulizaLimitKes(): Double? =
            dataStore.data.first()[AppSettingsKeys.FULIZA_LIMIT_KES]?.toDouble()

        suspend fun setFulizaLimitKes(limitKes: Double) {
            val normalized = limitKes.coerceAtLeast(0.0).toLong()
            dataStore.edit { prefs ->
                prefs[AppSettingsKeys.FULIZA_LIMIT_KES] = normalized
            }
        }

        suspend fun clearFulizaLimitKes() {
            dataStore.edit { prefs ->
                prefs.remove(AppSettingsKeys.FULIZA_LIMIT_KES)
            }
        }

        // ── Fuliza outstanding balance (SMS-derived) ─────────────────────────────

        fun fulizaOutstandingKesFlow(): Flow<Double?> =
            dataStore.data.map { prefs ->
                prefs[AppSettingsKeys.FULIZA_OUTSTANDING_KES_CENTS]?.let { it.toDouble() / 100.0 }
            }

        suspend fun getFulizaOutstandingKes(): Double? =
            dataStore.data.first()[AppSettingsKeys.FULIZA_OUTSTANDING_KES_CENTS]
                ?.let { it.toDouble() / 100.0 }

        /** Persist the latest Fuliza outstanding balance sourced from an SMS. */
        suspend fun setFulizaOutstandingKes(outstandingKes: Double) {
            val cents = (outstandingKes * 100).toLong().coerceAtLeast(0L)
            dataStore.edit { prefs ->
                prefs[AppSettingsKeys.FULIZA_OUTSTANDING_KES_CENTS] = cents
            }
        }

        suspend fun clearFulizaOutstandingKes() {
            dataStore.edit { prefs ->
                prefs.remove(AppSettingsKeys.FULIZA_OUTSTANDING_KES_CENTS)
            }
        }

        companion object {
            suspend fun areNotificationsEnabled(context: Context): Boolean {
                return context.appSettingsDataStore.data.first()[AppSettingsKeys.NOTIFICATIONS_ENABLED] ?: true
            }
        }
    }
