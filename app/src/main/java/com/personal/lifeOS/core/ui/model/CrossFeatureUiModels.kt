package com.personal.lifeOS.core.ui.model

enum class SyncStatusUiModel {
    LOCAL_ONLY,
    QUEUED,
    SYNCING,
    SYNCED,
    FAILED,
    CONFLICT,
    TOMBSTONED,
    UNKNOWN,
    ;

    companion object {
        fun fromRaw(raw: String?): SyncStatusUiModel {
            return entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: UNKNOWN
        }
    }
}

data class ImportHealthUiModel(
    val pendingReviewCount: Int = 0,
    val duplicateCount: Int = 0,
    val parseFailureCount: Int = 0,
    val lastImportSummary: String? = null,
    val latestImportAt: Long? = null,
)

data class AssistantActionProposalUiModel(
    val title: String,
    val summary: String,
    val riskLabel: String,
)

data class FreshnessUiModel(
    val label: String,
    val supportingLabel: String? = null,
    val isStale: Boolean = false,
)

data class UpdateNudgeUiModel(
    val title: String,
    val summary: String,
    val required: Boolean,
)
