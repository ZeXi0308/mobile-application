package com.sketchsync

import android.app.Application
import android.content.Context
import dagger.hilt.android.HiltAndroidApp

/**
 * SketchSync Application class
 * 使用Hilt进行依赖注入
 */
@HiltAndroidApp
class SketchSyncApplication : Application() {
    
    companion object {
        lateinit var appContext: Context
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        // 初始化日志、分析等
    }
}
