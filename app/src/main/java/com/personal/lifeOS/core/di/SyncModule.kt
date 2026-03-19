package com.personal.lifeOS.core.di

import com.personal.lifeOS.core.sync.CloudSyncDispatcher
import com.personal.lifeOS.core.sync.DefaultSyncConflictResolver
import com.personal.lifeOS.core.sync.DefaultSyncCoordinator
import com.personal.lifeOS.core.sync.DefaultSyncStatusTracker
import com.personal.lifeOS.core.sync.LogSyncTelemetry
import com.personal.lifeOS.core.sync.RoomSyncQueueStore
import com.personal.lifeOS.core.sync.SyncConflictResolver
import com.personal.lifeOS.core.sync.SyncCoordinator
import com.personal.lifeOS.core.sync.SyncDispatcher
import com.personal.lifeOS.core.sync.SyncQueueStore
import com.personal.lifeOS.core.sync.SyncStatusTracker
import com.personal.lifeOS.core.sync.SyncTelemetry
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {
    @Binds
    @Singleton
    abstract fun bindSyncQueueStore(impl: RoomSyncQueueStore): SyncQueueStore

    @Binds
    @Singleton
    abstract fun bindSyncDispatcher(impl: CloudSyncDispatcher): SyncDispatcher

    @Binds
    @Singleton
    abstract fun bindSyncCoordinator(impl: DefaultSyncCoordinator): SyncCoordinator

    @Binds
    @Singleton
    abstract fun bindSyncStatusTracker(impl: DefaultSyncStatusTracker): SyncStatusTracker

    @Binds
    @Singleton
    abstract fun bindSyncConflictResolver(impl: DefaultSyncConflictResolver): SyncConflictResolver

    @Binds
    @Singleton
    abstract fun bindSyncTelemetry(impl: LogSyncTelemetry): SyncTelemetry
}
