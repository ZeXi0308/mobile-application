package com.sketchsync.data.repository

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.sketchsync.data.model.ChatMessage
import com.sketchsync.data.model.MessageType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 聊天仓库
 * 处理实时消息发送和接收
 */
@Singleton
class ChatRepository @Inject constructor(
    private val database: FirebaseDatabase
) {
    private val chatsRef = database.reference.child("chats")
    
    /**
     * 发送聊天消息
     */
    suspend fun sendMessage(roomId: String, message: ChatMessage): Result<String> {
        return try {
            val messageRef = chatsRef.child(roomId).push()
            val messageWithId = message.copy(id = messageRef.key ?: message.id)
            messageRef.setValue(messageWithId.toMap()).await()
            Result.success(messageRef.key ?: message.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 发送系统消息（加入/离开房间）
     */
    suspend fun sendSystemMessage(roomId: String, content: String): Result<String> {
        val message = ChatMessage(
            roomId = roomId,
            content = content,
            type = MessageType.SYSTEM
        )
        return sendMessage(roomId, message)
    }
    
    /**
     * 发送猜词消息（Pictionary游戏）
     */
    suspend fun sendGuessMessage(
        roomId: String, 
        userId: String, 
        userName: String, 
        guess: String,
        isCorrect: Boolean
    ): Result<String> {
        val message = ChatMessage(
            roomId = roomId,
            userId = userId,
            userName = userName,
            content = guess,
            type = if (isCorrect) MessageType.CORRECT_GUESS else MessageType.GUESS,
            isCorrectGuess = isCorrect
        )
        return sendMessage(roomId, message)
    }
    
    /**
     * 监听房间消息
     */
    fun observeMessages(roomId: String): Flow<ChatMessage> = callbackFlow {
        val messagesRef = chatsRef.child(roomId)
        
        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    val data = snapshot.value as? Map<String, Any?> ?: return
                    val message = ChatMessage.fromMap(data, snapshot.key ?: "")
                    trySend(message)
                } catch (e: Exception) {
                    // 忽略解析错误
                }
            }
            
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        messagesRef.addChildEventListener(listener)
        awaitClose { messagesRef.removeEventListener(listener) }
    }
    
    /**
     * 获取最近的消息
     */
    suspend fun getRecentMessages(roomId: String, limit: Int = 50): Result<List<ChatMessage>> {
        return try {
            val snapshot = chatsRef.child(roomId)
                .orderByChild("timestamp")
                .limitToLast(limit)
                .get()
                .await()
            
            val messages = mutableListOf<ChatMessage>()
            snapshot.children.forEach { child ->
                try {
                    @Suppress("UNCHECKED_CAST")
                    val data = child.value as? Map<String, Any?> ?: return@forEach
                    messages.add(ChatMessage.fromMap(data, child.key ?: ""))
                } catch (e: Exception) {
                    // 忽略
                }
            }
            
            Result.success(messages.sortedBy { it.timestamp })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 清空房间聊天记录
     */
    suspend fun clearMessages(roomId: String): Result<Unit> {
        return try {
            chatsRef.child(roomId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
