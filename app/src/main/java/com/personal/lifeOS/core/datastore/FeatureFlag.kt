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
    ;

    companion object {
        private val byKey = entries.associateBy(FeatureFlag::key)

        fun fromKey(key: String): FeatureFlag? = byKey[key]
    }
}
