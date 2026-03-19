package com.personal.lifeOS.bootstrap

enum class StartDestination(val route: String) {
    AUTH("auth"),
    HOME("dashboard"),
}

data class BootstrapResult(
    val startDestination: StartDestination,
    val requiresBiometricRelock: Boolean,
    val shouldCheckForUpdates: Boolean,
)

data class AppBootstrapUiState(
    val isLoading: Boolean = true,
    val result: BootstrapResult? = null,
)
