package com.personal.lifeOS.features.profile.domain.model

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val avatarInitials: String = "?",
    val profilePicUri: String = "",
    val memberSince: Long = 0L, // epoch millis of first profile creation
    val isBiometricEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
)
