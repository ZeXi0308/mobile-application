package com.sketchsync

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * SketchSync Application class
 * 使用Hilt进行依赖注入
 */
@HiltAndroidApp
class SketchSyncApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // 初始化日志、分析等
    }
}
