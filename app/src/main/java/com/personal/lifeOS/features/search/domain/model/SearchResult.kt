package com.personal.lifeOS.features.search.domain.model

data class SearchResult(
    val id: String,
    val source: SearchSource,
    val title: String,
    val subtitle: String,
    val timestamp: Long,
)

enum class SearchSource {
    TRANSACTION,
    TASK,
    EVENT,
    BUDGET,
    INCOME,
    RECURRING_RULE,
}
