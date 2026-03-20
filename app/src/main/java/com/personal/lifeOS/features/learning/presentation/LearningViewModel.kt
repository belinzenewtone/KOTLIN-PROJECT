package com.personal.lifeOS.features.learning.presentation

import androidx.lifecycle.ViewModel
import com.personal.lifeOS.features.learning.domain.model.LearningCategory
import com.personal.lifeOS.features.learning.domain.model.LearningSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class LearningUiState(
    val sessions: List<LearningSession> = emptyList(),
    val selectedCategory: LearningCategory? = null,
    val isLoading: Boolean = false,
)

@HiltViewModel
class LearningViewModel
    @Inject
    constructor() : ViewModel() {

        private val _uiState = MutableStateFlow(LearningUiState(sessions = seedSessions()))
        val uiState: StateFlow<LearningUiState> = _uiState.asStateFlow()

        fun selectCategory(category: LearningCategory?) {
            _uiState.update { it.copy(selectedCategory = category) }
        }

        fun markCompleted(sessionId: Long) {
            _uiState.update { state ->
                state.copy(
                    sessions = state.sessions.map { s ->
                        if (s.id == sessionId) s.copy(isCompleted = true, progress = 1f) else s
                    },
                )
            }
        }

        fun updateProgress(sessionId: Long, progress: Float) {
            _uiState.update { state ->
                state.copy(
                    sessions = state.sessions.map { s ->
                        if (s.id == sessionId) s.copy(progress = progress.coerceIn(0f, 1f)) else s
                    },
                )
            }
        }

        /** Seed content — replace with Room DAO calls once entity + migration are added. */
        private fun seedSessions(): List<LearningSession> = listOf(
            LearningSession(1, "Budgeting with the 50/30/20 Rule", LearningCategory.FINANCE,
                "Learn how to split income into needs, wants, and savings.", 15),
            LearningSession(2, "Understanding MPESA Charges", LearningCategory.FINANCE,
                "Break down transaction fees and how to minimise them.", 10),
            LearningSession(3, "Deep Work: Single-Task Your Way to Peak Productivity", LearningCategory.PRODUCTIVITY,
                "How to achieve flow state and protect your focus hours.", 20),
            LearningSession(4, "Inbox Zero in 15 Minutes a Day", LearningCategory.PRODUCTIVITY,
                "A system for processing email without losing time.", 12),
            LearningSession(5, "4-7-8 Breathing for Stress", LearningCategory.WELLNESS,
                "A simple breathing technique that activates the parasympathetic system.", 8),
            LearningSession(6, "Sleep Hygiene Essentials", LearningCategory.WELLNESS,
                "Science-backed habits for consistent, restorative sleep.", 18),
            LearningSession(7, "What is Compound Interest?", LearningCategory.FINANCE,
                "How money grows over time and why starting early matters.", 10),
            LearningSession(8, "Morning Journaling for Clarity", LearningCategory.MINDFULNESS,
                "A 5-minute daily journaling practice to start the day with intention.", 7),
            LearningSession(9, "Git Basics Every Developer Should Know", LearningCategory.TECHNOLOGY,
                "Branches, commits, and merging without fear.", 25),
            LearningSession(10, "Introduction to AI Tools for Personal Productivity", LearningCategory.TECHNOLOGY,
                "Practical ways to use AI assistants in your daily workflow.", 20),
        )
    }
