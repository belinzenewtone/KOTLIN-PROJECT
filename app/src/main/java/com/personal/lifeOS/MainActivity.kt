@file:Suppress("MaxLineLength")

package com.personal.lifeOS

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.bootstrap.AppBootstrapViewModel
import com.personal.lifeOS.core.preferences.AppSettingsStore
import com.personal.lifeOS.core.preferences.ThemePreferences
import com.personal.lifeOS.navigation.LifeOSNavHost
import com.personal.lifeOS.ui.theme.AppThemeMode
import com.personal.lifeOS.ui.theme.LifeOSTheme
import com.personal.lifeOS.ui.splash.PersonalOsSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject
    lateinit var themePreferences: ThemePreferences

    @Inject
    lateinit var appSettingsStore: AppSettingsStore
    private var isSecureFlagEnabled: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by themePreferences.themeModeFlow()
                .collectAsState(initial = AppThemeMode.SYSTEM)
            val bootstrapViewModel: AppBootstrapViewModel = hiltViewModel()
            val bootstrapState by bootstrapViewModel.uiState.collectAsState()
            val biometricEnabled by appSettingsStore.biometricEnabledFlow().collectAsState(initial = false)
            val minSplashElapsed by
                produceState(initialValue = false) {
                    delay(1200)
                    value = true
                }

            LifeOSTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val shouldShowSplash = !minSplashElapsed || bootstrapState.isLoading || bootstrapState.result == null
                    Crossfade(
                        targetState = shouldShowSplash,
                        animationSpec = tween(durationMillis = 350, easing = EaseInOut),
                        label = "bootstrapCrossfade",
                    ) { loading ->
                        if (loading) {
                            PersonalOsSplashScreen()
                        } else {
                            LifeOSNavHost(
                                biometricEnabled = biometricEnabled && bootstrapState.result!!.requiresBiometricRelock,
                                startDestination = bootstrapState.result!!.startDestination.route,
                                shouldCheckForUpdates = bootstrapState.result!!.shouldCheckForUpdates,
                                onSensitiveScreenChanged = ::setSecureScreenEnabled,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun setSecureScreenEnabled(enabled: Boolean) {
        if (enabled == isSecureFlagEnabled) return
        isSecureFlagEnabled = enabled
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
