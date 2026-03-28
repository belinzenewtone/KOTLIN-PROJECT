package com.personal.lifeOS.core.datastore

enum class FeatureFlag(
    val key: String,
    val defaultEnabled: Boolean,
) {
    OTA_UPDATES(
        key = "ota_updates",
        defaultEnabled = true,
    ),
    ASSISTANT_ACTIONS(
        key = "assistant_actions",
        defaultEnabled = true,
    ),
    SMS_IMPORT(
        key = "sms_import",
        defaultEnabled = true,
    ),
    BACKGROUND_SYNC(
        key = "background_sync",
        defaultEnabled = true,
    ),
    HOME_RITUALS(
        key = "home_rituals",
        defaultEnabled = true,
    ),
    FINANCE_HEALTH_V2(
        key = "finance_health_v2",
        defaultEnabled = true,
    ),
    GROUPED_SEARCH(
        key = "grouped_search",
        defaultEnabled = true,
    ),
    EXPORT_CENTER_V2(
        key = "export_center_v2",
        defaultEnabled = true,
    ),
    REVIEW_RITUALS(
        key = "review_rituals",
        defaultEnabled = true,
    ),
    SYNC_CIRCUIT_BREAKER(
        key = "sync_circuit_breaker",
        defaultEnabled = true,
    ),
    ;

    companion object {
        private val byKey = entries.associateBy(FeatureFlag::key)

        fun fromKey(key: String): FeatureFlag? = byKey[key]
    }
}
