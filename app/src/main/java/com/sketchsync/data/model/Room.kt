package com.sketchsync.data.model

import com.google.firebase.Timestamp
import java.util.UUID

/**
 * 房间数据模型
 * 表示一个协作绘画房间
 */
data class Room(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val maxParticipants: Int = 8,
    val isPrivate: Boolean = false,
    val password: String? = null,
    val participants: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val isActive: Boolean = true,
    val lastActivity: Long = System.currentTimeMillis(),
    val gameMode: GameMode = GameMode.FREE_DRAW,
    // 成员角色映射 (userId -> role)
    val memberRoles: Map<String, String> = emptyMap()
) {
    /**
     * 获取用户角色
     */
    fun getUserRole(userId: String): RoomRole {
        return when {
            userId == creatorId -> RoomRole.OWNER
            memberRoles[userId] != null -> try {
                RoomRole.valueOf(memberRoles[userId]!!)
            } catch (e: Exception) {
                RoomRole.EDITOR
            }
            else -> RoomRole.EDITOR  // 默认为编辑者
        }
    }
    
    /**
     * 转换为Firebase可存储的Map格式
     */
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "name" to name,
        "creatorId" to creatorId,
        "creatorName" to creatorName,
        "createdAt" to createdAt,
        "maxParticipants" to maxParticipants,
        "isPrivate" to isPrivate,
        "password" to password,
        "participants" to participants,
        "participantNames" to participantNames,
        "isActive" to isActive,
        "lastActivity" to lastActivity,
        "gameMode" to gameMode.name,
        "memberRoles" to memberRoles
    )
    
    companion object {
        /**
         * 从Firebase Map数据创建Room对象
         */
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>, roomId: String = ""): Room {
            return Room(
                id = map["id"] as? String ?: roomId,
                name = map["name"] as? String ?: "",
                creatorId = map["creatorId"] as? String ?: "",
                creatorName = map["creatorName"] as? String ?: "",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                maxParticipants = (map["maxParticipants"] as? Number)?.toInt() ?: 8,
                isPrivate = map["isPrivate"] as? Boolean ?: false,
                password = map["password"] as? String,
                participants = map["participants"] as? List<String> ?: emptyList(),
                participantNames = map["participantNames"] as? Map<String, String> ?: emptyMap(),
                isActive = map["isActive"] as? Boolean ?: true,
                lastActivity = (map["lastActivity"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                gameMode = try {
                    GameMode.valueOf(map["gameMode"] as? String ?: "FREE_DRAW")
                } catch (e: Exception) {
                    GameMode.FREE_DRAW
                },
                memberRoles = map["memberRoles"] as? Map<String, String> ?: emptyMap()
            )
        }
    }
}

/**
 * 游戏模式
 */
enum class GameMode {
    FREE_DRAW,    // 自由绘画
    PICTIONARY    // 你画我猜
}

/**
 * 房间成员角色
 */
enum class RoomRole {
    OWNER,     // 房主：完全控制
    EDITOR,    // 编辑者：可绘图
    VIEWER     // 观看者：只读
}

/**
 * 参与者信息
 */
data class Participant(
    val odatabaseId: String = "",
    val displayName: String = "",
    val avatarUrl: String? = null,
    val joinedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val isMuted: Boolean = false,
    val cursorX: Float = 0f,
    val cursorY: Float = 0f
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to odatabaseId,
        "displayName" to displayName,
        "avatarUrl" to avatarUrl,
        "joinedAt" to joinedAt,
        "isActive" to isActive,
        "isMuted" to isMuted,
        "cursorX" to cursorX,
        "cursorY" to cursorY
    )
    
    companion object {
        fun fromMap(map: Map<String, Any?>, odatabaseId: String = ""): Participant {
            return Participant(
                odatabaseId = map["userId"] as? String ?: odatabaseId,
                displayName = map["displayName"] as? String ?: "",
                avatarUrl = map["avatarUrl"] as? String,
                joinedAt = (map["joinedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                isActive = map["isActive"] as? Boolean ?: true,
                isMuted = map["isMuted"] as? Boolean ?: false,
                cursorX = (map["cursorX"] as? Number)?.toFloat() ?: 0f,
                cursorY = (map["cursorY"] as? Number)?.toFloat() ?: 0f
            )
        }
    }
}
