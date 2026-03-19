package com.personal.lifeOS.core.sync

import com.personal.lifeOS.core.database.entity.SyncJobEntity

interface SyncDispatcher {
    suspend fun dispatch(job: SyncJobEntity): Result<Unit>
}
