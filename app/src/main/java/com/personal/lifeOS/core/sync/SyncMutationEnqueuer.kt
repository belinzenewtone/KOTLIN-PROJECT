package com.personal.lifeOS.core.sync

import com.personal.lifeOS.core.sync.model.SyncJobType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncMutationEnqueuer
    @Inject
    constructor(
        private val queueStore: SyncQueueStore,
    ) {
        suspend fun enqueueUpsert(
            entityType: String,
            entityId: String,
        ) {
            enqueue(
                entityType = entityType,
                entityId = entityId,
                operation = "UPSERT",
            )
        }

        suspend fun enqueueDelete(
            entityType: String,
            entityId: String,
        ) {
            enqueue(
                entityType = entityType,
                entityId = entityId,
                operation = "DELETE",
            )
        }

        private suspend fun enqueue(
            entityType: String,
            entityId: String,
            operation: String,
        ) {
            val payload =
                buildString {
                    append("{")
                    append("\"operation\":\"")
                    append(operation)
                    append("\",")
                    append("\"timestamp\":")
                    append(System.currentTimeMillis())
                    append("}")
                }
            queueStore.enqueue(
                type = SyncJobType.PUSH_ALL,
                entityType = entityType,
                entityId = entityId,
                payload = payload,
            )
        }
    }
