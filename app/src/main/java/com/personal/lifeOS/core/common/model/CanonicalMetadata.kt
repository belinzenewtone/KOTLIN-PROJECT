package com.personal.lifeOS.core.common.model

interface CanonicalMetadata {
    val createdAt: Long
    val updatedAt: Long
    val syncState: SyncState
    val recordSource: RecordSource
    val deletedAt: Long?
    val revision: Long
}
