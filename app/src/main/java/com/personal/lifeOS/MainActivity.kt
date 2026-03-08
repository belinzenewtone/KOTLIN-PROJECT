package com.personal.lifeOS

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.personal.lifeOS.core.preferences.AppSettingsStore
import com.personal.lifeOS.core.preferences.ThemePreferences
import com.personal.lifeOS.navigation.LifeOSNavHost
import com.personal.lifeOS.ui.theme.AppThemeMode
import com.personal.lifeOS.ui.theme.LifeOSTheme
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
            val biometricEnabled by appSettingsStore.biometricEnabledFlow()
                .collectAsState(initial = false)

            LifeOSTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LifeOSNavHost(biometricEnabled = biometricEnabled)
                }
            }
        }
    }
}
