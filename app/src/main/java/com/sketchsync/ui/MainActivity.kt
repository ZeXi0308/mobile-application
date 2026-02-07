package com.sketchsync.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.sketchsync.ui.navigation.SketchSyncNavigation
import com.sketchsync.ui.theme.SketchSyncTheme
import com.sketchsync.ui.theme.ThemeMode
import dagger.hilt.android.AndroidEntryPoint

/**
 * 主Activity
 * 使用Jetpack Compose构建UI
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            var themeMode by rememberSaveable { mutableStateOf(ThemeMode.DAY) }
            SketchSyncTheme(
                darkTheme = themeMode == ThemeMode.NIGHT,
                dynamicColor = false
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SketchSyncNavigation(
                        themeMode = themeMode,
                        onThemeModeChange = { themeMode = it }
                    )
                }
            }
        }
    }
}
