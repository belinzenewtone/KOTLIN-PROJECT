package com.personal.lifeOS.features.insights.domain.model

data class DeterministicInsightInput(
    val nowMillis: Long,
    val pendingTasks: List<InsightTaskSnapshot>,
    val recentTransactions: List<InsightTransactionSnapshot>,
)

data class InsightTaskSnapshot(
    val title: String,
    val deadline: Long?,
)

data class InsightTransactionSnapshot(
    val amount: Double,
    val category: String,
    val date: Long,
)

data class DeterministicInsightDraft(
    val kind: String,
    val title: String,
    val body: String,
    val confidence: Double,
)
