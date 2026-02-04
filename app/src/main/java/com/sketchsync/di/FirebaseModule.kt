package com.sketchsync.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Firebase依赖注入模块
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
    
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }
    
    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase {
        // 使用明确的数据库URL确保正确连接
        // 注意：如果你的Firebase项目在不同区域，需要修改这个URL
        val databaseUrl = "https://sketchsync-8c3e1-default-rtdb.firebaseio.com"
        return FirebaseDatabase.getInstance(databaseUrl).apply {
            // 开启离线持久化
            setPersistenceEnabled(true)
        }
    }
    
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }
    
    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return FirebaseStorage.getInstance()
    }
}
