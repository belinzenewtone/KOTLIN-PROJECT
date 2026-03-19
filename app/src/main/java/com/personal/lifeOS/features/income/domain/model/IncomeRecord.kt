package com.personal.lifeOS.features.income.domain.model

data class IncomeRecord(
    val id: Long = 0L,
    val amount: Double,
    val source: String,
    val date: Long = System.currentTimeMillis(),
    val note: String = "",
    val isRecurring: Boolean = false,
)
