package com.personal.lifeOS.features.export.domain.model

enum class ExportFormat {
    JSON,
    CSV,
}

enum class ExportDomain {
    ALL,
    TRANSACTIONS,
    TASKS,
    EVENTS,
    BUDGETS,
    INCOMES,
    RECURRING_RULES,
    MERCHANT_RULES,
}

data class ExportDateRange(
    val from: Long,
    val to: Long,
)

data class ExportRequest(
    val format: ExportFormat,
    val domain: ExportDomain,
    val dateRange: ExportDateRange? = null,
    val encryptionPassphrase: String? = null,
)

data class ExportPreview(
    val request: ExportRequest,
    val perDomainCount: Map<ExportDomain, Int>,
    val totalItems: Int,
)

data class ExportResult(
    val filePath: String,
    val itemCount: Int,
    val mimeType: String,
    val format: ExportFormat,
    val domain: ExportDomain,
    val encrypted: Boolean,
    val exportedAt: Long,
)

data class ExportHistoryItem(
    val id: Long,
    val format: ExportFormat,
    val domain: ExportDomain,
    val dateRange: ExportDateRange?,
    val filePath: String?,
    val itemCount: Int,
    val encrypted: Boolean,
    val status: String,
    val errorMessage: String?,
    val exportedAt: Long,
)
