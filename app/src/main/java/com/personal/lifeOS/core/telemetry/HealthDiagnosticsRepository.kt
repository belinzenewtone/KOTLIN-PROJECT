package com.personal.lifeOS.core.telemetry

import com.personal.lifeOS.core.database.dao.ImportAuditDao
import com.personal.lifeOS.core.datastore.FeatureFlag
import com.personal.lifeOS.core.datastore.FeatureFlagStore
import com.personal.lifeOS.core.security.AuthSessionStore
import com.personal.lifeOS.core.sync.SyncStatusTracker
import com.personal.lifeOS.core.update.AppUpdateInfo
import com.personal.lifeOS.core.update.UpdateDiagnosticsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class SyncHealthSummary(
    val queued: Int = 0,
    val syncing: Int = 0,
    val failed: Int = 0,
    val conflict: Int = 0,
    val latestJobUpdatedAt: Long? = null,
)

data class ImportHealthSummary(
    val imported: Int = 0,
    val duplicate: Int = 0,
    val parseFailed: Int = 0,
    val pending: Int = 0,
    val ignored: Int = 0,
    val recovered: Int = 0,
    val latestImportAt: Long? = null,
)

@Singleton
class HealthDiagnosticsRepository
    @Inject
    constructor(
        private val syncStatusTracker: SyncStatusTracker,
        private val importAuditDao: ImportAuditDao,
        private val updateDiagnosticsRepository: UpdateDiagnosticsRepository,
        private val featureFlagStore: FeatureFlagStore,
        private val authSessionStore: AuthSessionStore,
    ) {
        fun observeSyncHealth(): Flow<SyncHealthSummary> {
            return syncStatusTracker.observeQueueState().map { jobs ->
                SyncHealthSummary(
                    queued = jobs.count { it.status == "QUEUED" },
                    syncing = jobs.count { it.status == "SYNCING" },
                    failed = jobs.count { it.status == "FAILED" },
                    conflict = jobs.count { it.status == "CONFLICT" },
                    latestJobUpdatedAt = jobs.maxOfOrNull { it.updatedAt },
                )
            }
        }

        fun observeImportHealth(): Flow<ImportHealthSummary> {
            val userId = authSessionStore.getUserId().ifBlank { "local" }
            return importAuditDao.observeByUser(userId).map { entries ->
                val importedOutcomes =
                    setOf(
                        "imported",
                        "import_realtime",
                        "imported_realtime",
                    )
                val duplicateOutcomes =
                    setOf(
                        "duplicate",
                        "duplicate_detected",
                    )
                val parseFailureOutcomes =
                    setOf(
                        "parse_failed",
                        "import_failed",
                    )
                val pendingOutcomes =
                    setOf(
                        "candidate_pending",
                        "defer_to_batch",
                        "imported_batch_pending",
                        "quarantine_for_review",
                        "imported_quarantine",
                    )
                val ignoredOutcomes =
                    setOf(
                        "ignored_irrelevant",
                        "ignored_not_mpesa",
                    )
                ImportHealthSummary(
                    imported = entries.count { importedOutcomes.contains(it.outcome.lowercase()) },
                    duplicate = entries.count { duplicateOutcomes.contains(it.outcome.lowercase()) },
                    parseFailed = entries.count { parseFailureOutcomes.contains(it.outcome.lowercase()) },
                    pending = entries.count { pendingOutcomes.contains(it.outcome.lowercase()) },
                    ignored = entries.count { ignoredOutcomes.contains(it.outcome.lowercase()) },
                    recovered = entries.count { it.outcome.equals("recovered_from_backfill", ignoreCase = true) },
                    latestImportAt = entries.maxOfOrNull { it.importedAt },
                )
            }
        }

        fun observeLatestUpdateInfo(): Flow<AppUpdateInfo?> {
            return updateDiagnosticsRepository.observeLatest()
        }

        suspend fun featureFlagSnapshot(): Map<FeatureFlag, Boolean> {
            return featureFlagStore.snapshot()
        }
    }
