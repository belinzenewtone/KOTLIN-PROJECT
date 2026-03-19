package com.personal.lifeOS.core.update

import com.personal.lifeOS.core.database.dao.AppUpdateInfoDao
import com.personal.lifeOS.core.database.entity.AppUpdateInfoEntity
import com.personal.lifeOS.core.security.AuthSessionStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateDiagnosticsRepository
    @Inject
    constructor(
        private val dao: AppUpdateInfoDao,
        private val authSessionStore: AuthSessionStore,
    ) {
        suspend fun save(info: AppUpdateInfo) {
            val userId = authSessionStore.getUserId().ifBlank { "local" }
            dao.insert(
                AppUpdateInfoEntity(
                    id = info.checkedAt,
                    userId = userId,
                    versionCode = info.versionCode,
                    versionName = info.versionName,
                    isRequired = info.required,
                    downloadUrl = info.downloadUrl,
                    checksumSha256 = info.checksumSha256,
                    checkedAt = info.checkedAt,
                ),
            )
        }

        fun observeLatest(): Flow<AppUpdateInfo?> {
            val userId = authSessionStore.getUserId().ifBlank { "local" }
            return dao.observeLatest(userId).map { entity ->
                entity?.let {
                    AppUpdateInfo(
                        versionCode = it.versionCode,
                        versionName = it.versionName,
                        required = it.isRequired,
                        downloadUrl = it.downloadUrl,
                        checksumSha256 = it.checksumSha256,
                        checkedAt = it.checkedAt,
                    )
                }
            }
        }
    }
