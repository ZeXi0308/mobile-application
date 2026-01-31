package com.sketchsync.data.model

import java.util.UUID

/**
 * 聊天消息数据模型
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val roomId: String = "",
    val userId: String = "",
    val userName: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val type: MessageType = MessageType.TEXT,
    val isCorrectGuess: Boolean = false  // 用于Pictionary游戏中正确答案的标记
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "roomId" to roomId,
        "userId" to userId,
        "userName" to userName,
        "content" to content,
        "timestamp" to timestamp,
        "type" to type.name,
        "isCorrectGuess" to isCorrectGuess
    )
    
    companion object {
        fun fromMap(map: Map<String, Any?>, messageId: String = ""): ChatMessage {
            return ChatMessage(
                id = map["id"] as? String ?: messageId,
                roomId = map["roomId"] as? String ?: "",
                userId = map["userId"] as? String ?: "",
                userName = map["userName"] as? String ?: "",
                content = map["content"] as? String ?: "",
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                type = try {
                    MessageType.valueOf(map["type"] as? String ?: "TEXT")
                } catch (e: Exception) {
                    MessageType.TEXT
                },
                isCorrectGuess = map["isCorrectGuess"] as? Boolean ?: false
            )
        }
    }
}

/**
 * 消息类型
 */
enum class MessageType {
    TEXT,           // 普通文本
    SYSTEM,         // 系统消息（加入/离开房间）
    GUESS,          // 游戏猜词
    CORRECT_GUESS,  // 猜对了
    EMOJI           // 表情
}
