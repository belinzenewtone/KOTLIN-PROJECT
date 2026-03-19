package com.personal.lifeOS.core.sync.model

enum class SyncTrigger {
    APP_START,
    PERIODIC_WORK,
    USER_PULL_TO_REFRESH,
    USER_MANUAL_RETRY,
    NETWORK_RESTORED,
}
