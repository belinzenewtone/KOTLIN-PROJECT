package com.personal.lifeOS

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.personal.lifeOS.navigation.LifeOSNavHost
import com.personal.lifeOS.ui.theme.LifeOSTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LifeOSTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LifeOSNavHost()
                }
            }
        }
    }
}
