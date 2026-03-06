package com.personal.lifeOS.features.profile.domain.model

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val avatarInitials: String = "",
    val isBiometricEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val darkModeEnabled: Boolean = true
)
