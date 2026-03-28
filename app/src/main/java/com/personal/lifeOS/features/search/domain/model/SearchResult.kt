package com.personal.lifeOS.features.search.domain.model

data class SearchResult(
    val id: String,
    val source: SearchSource,
    val title: String,
    val subtitle: String,
    val timestamp: Long,
    val relevanceScore: Int = 0,
    val navigationTarget: String? = null,
)

enum class SearchSource {
    TRANSACTION,
    TASK,
    EVENT,
    BUDGET,
    INCOME,
    RECURRING_RULE,
    ;

    val groupLabel: String
        get() =
            when (this) {
                TRANSACTION,
                BUDGET,
                INCOME,
                -> "Finance"
                TASK -> "Tasks"
                EVENT -> "Calendar"
                RECURRING_RULE -> "Recurring"
            }
}
