package com.personal.lifeOS.features.insights.domain.model

data class InsightCard(
    val id: Long,
    val kind: String,
    val title: String,
    val body: String,
    val confidence: Double?,
    val isAiGenerated: Boolean,
    val freshUntil: Long?,
    val createdAt: Long,
)
