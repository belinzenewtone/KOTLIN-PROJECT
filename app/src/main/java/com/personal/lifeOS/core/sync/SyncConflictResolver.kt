package com.personal.lifeOS.core.sync

import javax.inject.Inject
import javax.inject.Singleton

data class SyncConflict(
    val entityType: String,
    val entityId: String,
    val reason: String,
)

sealed interface SyncResolution {
    data object KeepLocal : SyncResolution

    data object KeepRemote : SyncResolution

    data class Merge(val payload: String) : SyncResolution
}

interface SyncConflictResolver {
    suspend fun resolve(conflict: SyncConflict): SyncResolution
}

@Singleton
class DefaultSyncConflictResolver
    @Inject
    constructor() : SyncConflictResolver {
        override suspend fun resolve(conflict: SyncConflict): SyncResolution {
            // Conservative default until feature-level resolvers are plugged in.
            return SyncResolution.KeepRemote
        }
    }
