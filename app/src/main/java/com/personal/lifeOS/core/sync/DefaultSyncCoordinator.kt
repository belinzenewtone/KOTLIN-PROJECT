package com.personal.lifeOS.core.sync

import com.personal.lifeOS.core.database.entity.SyncJobEntity
import com.personal.lifeOS.core.sync.model.SyncJobType
import com.personal.lifeOS.core.sync.model.SyncTrigger
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultSyncCoordinator
    @Inject
    constructor(
        private val queueStore: SyncQueueStore,
        private val dispatcher: SyncDispatcher,
        private val backoffPolicy: SyncBackoffPolicy,
        private val telemetry: SyncTelemetry,
        private val circuitBreaker: SyncCircuitBreaker,
    ) : SyncCoordinator {
        override suspend fun enqueueDefault(trigger: SyncTrigger) {
            when (trigger) {
                SyncTrigger.APP_START,
                SyncTrigger.NETWORK_RESTORED,
                SyncTrigger.PERIODIC_WORK,
                SyncTrigger.USER_PULL_TO_REFRESH,
                -> {
                    queueStore.enqueue(
                        type = SyncJobType.PUSH_ALL,
                        entityType = "global",
                        entityId = trigger.name.lowercase(),
                    )
                    queueStore.enqueue(
                        type = SyncJobType.PULL_ALL,
                        entityType = "global",
                        entityId = trigger.name.lowercase(),
                    )
                }

                SyncTrigger.USER_MANUAL_RETRY -> {
                    queueStore.enqueue(
                        type = SyncJobType.REPAIR_ALL,
                        entityType = "global",
                        entityId = "manual_retry",
                    )
                }
            }
        }

        override suspend fun runPending(limit: Int) {
            val jobs = queueStore.dueJobs(limit)
            jobs.forEach { job -> processJob(job) }
            queueStore.pruneSynced(System.currentTimeMillis() - (24L * 60L * 60L * 1000L))
        }

        override fun observeQueue(): Flow<List<SyncJobEntity>> = queueStore.observeJobs()

        private suspend fun processJob(job: SyncJobEntity) {
            if (!circuitBreaker.shouldAllow()) {
                val blockedUntil = circuitBreaker.snapshot().openUntilMillis
                queueStore.markFailed(
                    job = job,
                    error = "Sync circuit breaker open",
                    nextRunAt = blockedUntil.takeIf { it > 0L } ?: backoffPolicy.nextRetryAt(job.attemptCount + 1),
                )
                telemetry.onJobFailed(job.jobType, "Sync circuit breaker open")
                return
            }
            queueStore.markSyncing(job)
            telemetry.onJobStarted(job.jobType)
            val result = runCatching { dispatcher.dispatch(job).getOrThrow() }
            result.onSuccess {
                queueStore.markSynced(job)
                circuitBreaker.onSuccess()
                telemetry.onJobSucceeded(job.jobType)
            }.onFailure { error ->
                val breakerState = circuitBreaker.onFailure()
                val nextRunAt = maxOf(backoffPolicy.nextRetryAt(job.attemptCount + 1), breakerState.openUntilMillis)
                queueStore.markFailed(job, error.message, nextRunAt)
                telemetry.onJobFailed(job.jobType, error.message)
            }
        }
    }
