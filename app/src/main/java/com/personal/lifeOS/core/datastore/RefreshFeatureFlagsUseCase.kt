package com.personal.lifeOS.core.datastore

import com.personal.lifeOS.core.network.FeatureFlagRemoteDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RefreshFeatureFlagsUseCase
    @Inject
    constructor(
        private val remoteDataSource: FeatureFlagRemoteDataSource,
        private val store: FeatureFlagStore,
    ) {
        suspend operator fun invoke(): Result<Unit> {
            return remoteDataSource.fetchFlags().mapCatching { remoteFlags ->
                store.applyRemoteValues(remoteFlags)
                Unit
            }
        }
    }
