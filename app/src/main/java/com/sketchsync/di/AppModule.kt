package com.sketchsync.di

import android.content.Context
import com.sketchsync.data.repository.AuthRepository
import com.sketchsync.data.repository.DrawingRepository
import com.sketchsync.data.repository.RoomRepository
import com.sketchsync.util.VoiceChatManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 应用级依赖注入模块
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideVoiceChatManager(
        @ApplicationContext context: Context
    ): VoiceChatManager {
        return VoiceChatManager(context)
    }
}
