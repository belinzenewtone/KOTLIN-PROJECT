package com.personal.lifeOS.core.common.model

enum class SyncState {
    LOCAL_ONLY,
    QUEUED,
    SYNCING,
    SYNCED,
    FAILED,
    CONFLICT,
    TOMBSTONED,
}
