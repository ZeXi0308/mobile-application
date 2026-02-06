package com.sketchsync.data.repository

import android.util.Log
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.sketchsync.data.model.GameMode
import com.sketchsync.data.model.Participant
import com.sketchsync.data.model.Room
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "RoomRepository"

/**
 * 房间仓库
 * 处理房间创建、加入、离开等操作
 */
@Singleton
class RoomRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val database: FirebaseDatabase
) {
    private val roomsCollection = firestore.collection("rooms")
    private val roomsRef = database.reference.child("rooms")
    private val presenceRef = database.reference.child("presence")
    private val connectedRef = database.reference.child(".info/connected")
    
    /**
     * 创建新房间
     */
    suspend fun createRoom(
        name: String,
        creatorId: String,
        creatorName: String,
        maxParticipants: Int = 8,
        isPrivate: Boolean = false,
        password: String? = null,
        gameMode: GameMode = GameMode.FREE_DRAW
    ): Result<Room> {
        return try {
            val room = Room(
                name = name,
                creatorId = creatorId,
                creatorName = creatorName,
                maxParticipants = maxParticipants,
                isPrivate = isPrivate,
                password = password,
                participants = listOf(creatorId),
                participantNames = mapOf(creatorId to creatorName),
                gameMode = gameMode
            )
            
            // 保存到Firestore
            roomsCollection.document(room.id).set(room.toMap()).await()
            
            // 在Realtime Database中创建房间数据（用于实时同步）
            roomsRef.child(room.id).child("metadata").setValue(
                mapOf(
                    "createdAt" to room.createdAt,
                    "creatorId" to creatorId
                )
            ).await()
            
            Result.success(room)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取房间详情
     */
    suspend fun getRoom(roomId: String): Result<Room> {
        return try {
            val doc = roomsCollection.document(roomId).get().await()
            if (doc.exists()) {
                val data = doc.data ?: emptyMap()
                Result.success(Room.fromMap(data, roomId))
            } else {
                Result.failure(Exception("房间不存在"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取活跃房间列表
     */
    fun observeActiveRooms(): Flow<List<Room>> = callbackFlow {
        val listener = roomsCollection
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val rooms = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { Room.fromMap(it, doc.id) }
                } ?: emptyList()
                
                trySend(rooms.sortedByDescending { it.lastActivity })
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * 监听房间详情变化
     */
    fun observeRoom(roomId: String): Flow<Room?> = callbackFlow {
        val listener = roomsCollection.document(roomId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val room = snapshot?.data?.let { Room.fromMap(it, roomId) }
                trySend(room)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * 加入房间
     */
    suspend fun joinRoom(roomId: String, userId: String, userName: String, password: String? = null): Result<Room> {
        return try {
            val room = getRoom(roomId).getOrThrow()
            
            // 验证密码
            if (room.isPrivate && room.password != null && room.password != password) {
                return Result.failure(Exception("密码错误"))
            }
            
            // 检查人数限制
            if (room.participants.size >= room.maxParticipants) {
                return Result.failure(Exception("房间已满"))
            }
            
            // 检查是否已在房间中
            if (userId in room.participants) {
                return Result.success(room)
            }
            
            // 更新参与者列表
            val updatedParticipants = room.participants + userId
            val updatedNames = room.participantNames + (userId to userName)
            
            roomsCollection.document(roomId).update(
                mapOf(
                    "participants" to updatedParticipants,
                    "participantNames" to updatedNames,
                    "lastActivity" to System.currentTimeMillis()
                )
            ).await()
            
            Result.success(room.copy(
                participants = updatedParticipants,
                participantNames = updatedNames
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 离开房间
     */
    suspend fun leaveRoom(roomId: String, userId: String): Result<Unit> {
        return try {
            val room = getRoom(roomId).getOrThrow()
            
            val updatedParticipants = room.participants - userId
            val updatedNames = room.participantNames - userId
            
            if (updatedParticipants.isEmpty()) {
                // 房间没有人了，关闭房间
                roomsCollection.document(roomId).update(
                    mapOf(
                        "isActive" to false,
                        "participants" to emptyList<String>(),
                        "participantNames" to emptyMap<String, String>()
                    )
                ).await()
            } else {
                // 更新参与者列表
                val updates = mutableMapOf<String, Any>(
                    "participants" to updatedParticipants,
                    "participantNames" to updatedNames,
                    "lastActivity" to System.currentTimeMillis()
                )
                
                // 如果创建者离开，转移房主
                if (userId == room.creatorId && updatedParticipants.isNotEmpty()) {
                    val newCreator = updatedParticipants.first()
                    updates["creatorId"] = newCreator
                    updates["creatorName"] = updatedNames[newCreator] ?: ""
                }
                
                roomsCollection.document(roomId).update(updates).await()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 删除房间
     */
    suspend fun deleteRoom(roomId: String): Result<Unit> {
        return try {
            roomsCollection.document(roomId).delete().await()
            roomsRef.child(roomId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 设置成员角色
     */
    suspend fun setMemberRole(roomId: String, userId: String, role: String): Result<Unit> {
        return try {
            // 获取当前 memberRoles
            val room = getRoom(roomId).getOrThrow()
            val updatedRoles = room.memberRoles.toMutableMap()
            updatedRoles[userId] = role
            
            roomsCollection.document(roomId).update("memberRoles", updatedRoles).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 踢出成员
     */
    suspend fun kickMember(roomId: String, userId: String): Result<Unit> {
        return leaveRoom(roomId, userId)
    }
    
    /**
     * 搜索房间
     */
    suspend fun searchRooms(query: String): Result<List<Room>> {
        return try {
            val snapshot = roomsCollection
                .whereEqualTo("isActive", true)
                .whereEqualTo("isPrivate", false)
                .get()
                .await()
            
            val rooms = snapshot.documents.mapNotNull { doc ->
                doc.data?.let { Room.fromMap(it, doc.id) }
            }.filter { room ->
                room.name.contains(query, ignoreCase = true) ||
                room.id.contains(query, ignoreCase = true)
            }
            
            Result.success(rooms)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 通过ID直接加入房间
     */
    suspend fun joinRoomById(roomId: String, userId: String, userName: String): Result<Room> {
        return joinRoom(roomId, userId, userName, null)
    }
    
    /**
     * 设置用户在线状态（使用Firebase Realtime Database的onDisconnect实现自动离线）
     */
    fun setUserPresence(roomId: String, userId: String, userName: String) {
        Log.d(TAG, "setUserPresence: roomId=$roomId, userId=$userId, userName=$userName")
        val userPresenceRef = presenceRef.child(roomId).child(userId)
        
        // 立即设置在线状态
        val presenceData = mapOf(
            "online" to true,
            "userName" to userName,
            "lastSeen" to System.currentTimeMillis()
        )
        userPresenceRef.setValue(presenceData)
            .addOnSuccessListener { Log.d(TAG, "Presence set successfully for $userId in room $roomId") }
            .addOnFailureListener { e -> Log.e(TAG, "Failed to set presence: ${e.message}") }
        
        // 设置断开时自动移除（作为备份机制）
        userPresenceRef.onDisconnect().removeValue()
        
        // 监听连接状态变化，重新连接时恢复在线状态
        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                Log.d(TAG, "Connection state changed: connected=$connected")
                if (connected) {
                    // 重新连接时恢复在线状态
                    userPresenceRef.setValue(presenceData)
                    userPresenceRef.onDisconnect().removeValue()
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Connection listener cancelled: ${error.message}")
            }
        })
    }
    
    /**
     * 移除用户在线状态
     */
    fun removeUserPresence(roomId: String, userId: String) {
        Log.d(TAG, "removeUserPresence: roomId=$roomId, userId=$userId")
        presenceRef.child(roomId).child(userId).removeValue()
    }
    
    /**
     * 观察房间在线用户
     */
    fun observeRoomPresence(roomId: String): Flow<Map<String, String>> = callbackFlow {
        val listener = presenceRef.child(roomId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val onlineUsers = mutableMapOf<String, String>()
                snapshot.children.forEach { child ->
                    val userId = child.key ?: return@forEach
                    val userName = child.child("userName").getValue(String::class.java) ?: "用户"
                    val online = child.child("online").getValue(Boolean::class.java) ?: false
                    if (online) {
                        onlineUsers[userId] = userName
                    }
                }
                trySend(onlineUsers)
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        })
        
        awaitClose { presenceRef.child(roomId).removeEventListener(listener) }
    }
    
    /**
     * 观察所有房间的在线人数
     */
    fun observeAllRoomPresenceCounts(): Flow<Map<String, Int>> = callbackFlow {
        Log.d(TAG, "observeAllRoomPresenceCounts: Starting observer")
        val listener = presenceRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val counts = mutableMapOf<String, Int>()
                snapshot.children.forEach { roomSnapshot ->
                    val roomId = roomSnapshot.key ?: return@forEach
                    var count = 0
                    roomSnapshot.children.forEach { userSnapshot ->
                        val online = userSnapshot.child("online").getValue(Boolean::class.java) ?: false
                        if (online) count++
                    }
                    counts[roomId] = count
                }
                Log.d(TAG, "observeAllRoomPresenceCounts: Emitting counts=$counts")
                trySend(counts)
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "observeAllRoomPresenceCounts cancelled: ${error.message}")
                close(error.toException())
            }
        })
        
        awaitClose { 
            Log.d(TAG, "observeAllRoomPresenceCounts: Closing observer")
            presenceRef.removeEventListener(listener) 
        }
    }
}
