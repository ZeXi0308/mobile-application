package com.sketchsync.data.model

/**
 * 用户数据模型
 */
data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val avatarUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis(),
    val totalDrawings: Int = 0,
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "uid" to uid,
        "email" to email,
        "displayName" to displayName,
        "avatarUrl" to avatarUrl,
        "createdAt" to createdAt,
        "lastLoginAt" to lastLoginAt,
        "totalDrawings" to totalDrawings,
        "gamesPlayed" to gamesPlayed,
        "gamesWon" to gamesWon
    )
    
    companion object {
        fun fromMap(map: Map<String, Any?>, uid: String = ""): User {
            return User(
                uid = map["uid"] as? String ?: uid,
                email = map["email"] as? String ?: "",
                displayName = map["displayName"] as? String ?: "",
                avatarUrl = map["avatarUrl"] as? String,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                lastLoginAt = (map["lastLoginAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                totalDrawings = (map["totalDrawings"] as? Number)?.toInt() ?: 0,
                gamesPlayed = (map["gamesPlayed"] as? Number)?.toInt() ?: 0,
                gamesWon = (map["gamesWon"] as? Number)?.toInt() ?: 0
            )
        }
    }
}

/**
 * 认证状态
 */
sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}
