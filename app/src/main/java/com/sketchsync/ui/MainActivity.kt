package com.sketchsync.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.sketchsync.ui.navigation.SketchSyncNavigation
import com.sketchsync.ui.theme.SketchSyncTheme
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
            SketchSyncTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SketchSyncNavigation()
                }
            }
        }
    }
}
