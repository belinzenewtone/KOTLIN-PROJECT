package com.personal.lifeOS.features.learning.domain.model

/**
 * Core domain model for a learning session / module.
 *
 * Persisted via [LearningSessionEntity] (to be added to Room in a future migration);
 * for now the ViewModel operates with in-memory seed data.
 */
data class LearningSession(
    val id: Long,
    val title: String,
    val category: LearningCategory,
    val description: String,
    val durationMinutes: Int,
    val isCompleted: Boolean = false,
    val progress: Float = 0f,   // 0.0 – 1.0
    val createdAt: Long = System.currentTimeMillis(),
)

enum class LearningCategory(val label: String) {
    FINANCE("Finance"),
    PRODUCTIVITY("Productivity"),
    WELLNESS("Wellness"),
    TECHNOLOGY("Technology"),
    MINDFULNESS("Mindfulness"),
}
