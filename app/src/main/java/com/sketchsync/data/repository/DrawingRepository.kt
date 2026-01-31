package com.sketchsync.data.repository

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.sketchsync.data.model.DrawPath
import com.sketchsync.data.model.PathPoint
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 绘图仓库
 * 处理绘图数据的实时同步、存储和导出
 */
@Singleton
class DrawingRepository @Inject constructor(
    private val database: FirebaseDatabase,
    private val storage: FirebaseStorage
) {
    private val roomsRef = database.reference.child("rooms")
    
    // 节流：光标更新最小间隔
    private var lastCursorUpdate = 0L
    private val cursorUpdateInterval = 100L // 100ms
    
    /**
     * 发送绘图路径到服务器
     */
    suspend fun sendDrawPath(roomId: String, path: DrawPath): Result<String> {
        return try {
            val pathRef = roomsRef.child(roomId).child("paths").push()
            val pathWithId = path.copy(odatabaseId = pathRef.key ?: path.id)
            pathRef.setValue(pathWithId.toMap()).await()
            Result.success(pathRef.key ?: path.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 批量发送绘图路径（优化性能）
     */
    suspend fun sendDrawPaths(roomId: String, paths: List<DrawPath>): Result<Unit> {
        return try {
            val pathsRef = roomsRef.child(roomId).child("paths")
            val updates = paths.associate { path ->
                val key = pathsRef.push().key ?: path.id
                key to path.copy(odatabaseId = key).toMap()
            }
            pathsRef.updateChildren(updates as Map<String, Any>).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 监听房间中的新绘图路径
     */
    fun observeDrawPaths(roomId: String): Flow<DrawPath> = callbackFlow {
        val pathsRef = roomsRef.child(roomId).child("paths")
        
        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    val data = snapshot.value as? Map<String, Any?> ?: return
                    val path = DrawPath.fromMap(data, snapshot.key ?: "")
                    trySend(path)
                } catch (e: Exception) {
                    // 忽略解析错误
                }
            }
            
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // 路径通常不会被修改
            }
            
            override fun onChildRemoved(snapshot: DataSnapshot) {
                // 可以在这里处理撤销逻辑
            }
            
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        pathsRef.addChildEventListener(listener)
        awaitClose { pathsRef.removeEventListener(listener) }
    }
    
    /**
     * 获取房间所有绘图路径
     */
    suspend fun getAllPaths(roomId: String): Result<List<DrawPath>> {
        return try {
            val snapshot = roomsRef.child(roomId).child("paths").get().await()
            val paths = mutableListOf<DrawPath>()
            
            snapshot.children.forEach { child ->
                try {
                    @Suppress("UNCHECKED_CAST")
                    val data = child.value as? Map<String, Any?> ?: return@forEach
                    paths.add(DrawPath.fromMap(data, child.key ?: ""))
                } catch (e: Exception) {
                    // 忽略解析错误
                }
            }
            
            Result.success(paths.sortedBy { it.timestamp })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 清空画布（同时通知其他用户）
     */
    suspend fun clearCanvas(roomId: String): Result<Unit> {
        return try {
            // 删除所有路径
            roomsRef.child(roomId).child("paths").removeValue().await()
            // 发送清空事件
            roomsRef.child(roomId).child("clearEvent").setValue(
                mapOf("timestamp" to System.currentTimeMillis())
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 监听画布清空事件
     */
    fun observeClearEvents(roomId: String): Flow<Long> = callbackFlow {
        val clearRef = roomsRef.child(roomId).child("clearEvent")
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val timestamp = (snapshot.child("timestamp").value as? Number)?.toLong() ?: 0L
                if (timestamp > 0) {
                    trySend(timestamp)
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        clearRef.addValueEventListener(listener)
        awaitClose { clearRef.removeEventListener(listener) }
    }
    
    /**
     * 撤销最后一条路径
     */
    suspend fun undoLastPath(roomId: String, userId: String): Result<Unit> {
        return try {
            val pathsRef = roomsRef.child(roomId).child("paths")
            val snapshot = pathsRef.orderByChild("userId").equalTo(userId).limitToLast(1).get().await()
            
            snapshot.children.forEach { child ->
                child.ref.removeValue().await()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 更新用户光标位置（带节流）
     */
    fun updateCursorPosition(roomId: String, userId: String, x: Float, y: Float) {
        val now = System.currentTimeMillis()
        if (now - lastCursorUpdate < cursorUpdateInterval) {
            return
        }
        lastCursorUpdate = now
        
        roomsRef.child(roomId).child("cursors").child(userId).setValue(
            mapOf(
                "x" to x,
                "y" to y,
                "timestamp" to now
            )
        )
    }
    
    /**
     * 监听其他用户的光标位置
     */
    fun observeCursors(roomId: String, currentUserId: String): Flow<Map<String, Pair<Float, Float>>> = callbackFlow {
        val cursorsRef = roomsRef.child(roomId).child("cursors")
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val cursors = mutableMapOf<String, Pair<Float, Float>>()
                
                snapshot.children.forEach { child ->
                    val odatabaseId = child.key ?: return@forEach
                    if (odatabaseId == currentUserId) return@forEach
                    
                    val x = (child.child("x").value as? Number)?.toFloat() ?: 0f
                    val y = (child.child("y").value as? Number)?.toFloat() ?: 0f
                    cursors[odatabaseId] = Pair(x, y)
                }
                
                trySend(cursors)
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        cursorsRef.addValueEventListener(listener)
        awaitClose { cursorsRef.removeEventListener(listener) }
    }
    
    /**
     * 导出画布为图片并上传到Firebase Storage
     */
    suspend fun uploadCanvasImage(roomId: String, imageBytes: ByteArray): Result<String> {
        return try {
            val fileName = "${System.currentTimeMillis()}.png"
            val imageRef = storage.reference.child("drawings/$roomId/$fileName")
            
            imageRef.putBytes(imageBytes).await()
            val downloadUrl = imageRef.downloadUrl.await().toString()
            
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取房间保存的图片列表
     */
    suspend fun getSavedImages(roomId: String): Result<List<String>> {
        return try {
            val listResult = storage.reference.child("drawings/$roomId").listAll().await()
            val urls = listResult.items.map { it.downloadUrl.await().toString() }
            Result.success(urls)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
