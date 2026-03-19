package com.personal.lifeOS

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.bootstrap.AppBootstrapViewModel
import com.personal.lifeOS.core.preferences.AppSettingsStore
import com.personal.lifeOS.core.preferences.ThemePreferences
import com.personal.lifeOS.navigation.LifeOSNavHost
import com.personal.lifeOS.ui.theme.AppThemeMode
import com.personal.lifeOS.ui.theme.BackgroundDark
import com.personal.lifeOS.ui.theme.LifeOSTheme
import com.personal.lifeOS.ui.theme.Primary
import com.personal.lifeOS.ui.theme.TextSecondary
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var themePreferences: ThemePreferences

    @Inject
    lateinit var appSettingsStore: AppSettingsStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by themePreferences.themeModeFlow()
                .collectAsState(initial = AppThemeMode.SYSTEM)
            val bootstrapViewModel: AppBootstrapViewModel = hiltViewModel()
            val bootstrapState by bootstrapViewModel.uiState.collectAsState()
            val biometricEnabled by appSettingsStore.biometricEnabledFlow().collectAsState(initial = false)

            LifeOSTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (bootstrapState.isLoading || bootstrapState.result == null) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .background(BackgroundDark),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Primary)
                                Text(
                                    text = "Starting Personal OS...",
                                    color = TextSecondary,
                                    modifier = Modifier.align(Alignment.BottomCenter),
                                )
                            }
                        }
                    } else {
                        LifeOSNavHost(
                            biometricEnabled = biometricEnabled && bootstrapState.result!!.requiresBiometricRelock,
                            startDestination = bootstrapState.result!!.startDestination.route,
                            shouldCheckForUpdates = bootstrapState.result!!.shouldCheckForUpdates,
                        )
                    }
                }
            }
        }
    }
}
