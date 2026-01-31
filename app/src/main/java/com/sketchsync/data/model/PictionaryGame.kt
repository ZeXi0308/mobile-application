package com.sketchsync.data.model

import java.util.UUID

/**
 * Pictionary游戏数据模型
 */
data class PictionaryGame(
    val id: String = UUID.randomUUID().toString(),
    val roomId: String = "",
    val currentDrawerId: String = "",
    val currentDrawerName: String = "",
    val currentWord: String = "",
    val wordHint: String = "",  // 提示（显示部分字母）
    val timeRemaining: Int = 60,
    val totalTime: Int = 60,
    val round: Int = 1,
    val maxRounds: Int = 5,
    val scores: Map<String, Int> = emptyMap(),
    val guessedUsers: List<String> = emptyList(),
    val isActive: Boolean = false,
    val isPaused: Boolean = false,
    val turnOrder: List<String> = emptyList(),
    val startedAt: Long = 0
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "roomId" to roomId,
        "currentDrawerId" to currentDrawerId,
        "currentDrawerName" to currentDrawerName,
        "currentWord" to currentWord,
        "wordHint" to wordHint,
        "timeRemaining" to timeRemaining,
        "totalTime" to totalTime,
        "round" to round,
        "maxRounds" to maxRounds,
        "scores" to scores,
        "guessedUsers" to guessedUsers,
        "isActive" to isActive,
        "isPaused" to isPaused,
        "turnOrder" to turnOrder,
        "startedAt" to startedAt
    )
    
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>, gameId: String = ""): PictionaryGame {
            return PictionaryGame(
                id = map["id"] as? String ?: gameId,
                roomId = map["roomId"] as? String ?: "",
                currentDrawerId = map["currentDrawerId"] as? String ?: "",
                currentDrawerName = map["currentDrawerName"] as? String ?: "",
                currentWord = map["currentWord"] as? String ?: "",
                wordHint = map["wordHint"] as? String ?: "",
                timeRemaining = (map["timeRemaining"] as? Number)?.toInt() ?: 60,
                totalTime = (map["totalTime"] as? Number)?.toInt() ?: 60,
                round = (map["round"] as? Number)?.toInt() ?: 1,
                maxRounds = (map["maxRounds"] as? Number)?.toInt() ?: 5,
                scores = map["scores"] as? Map<String, Int> ?: emptyMap(),
                guessedUsers = map["guessedUsers"] as? List<String> ?: emptyList(),
                isActive = map["isActive"] as? Boolean ?: false,
                isPaused = map["isPaused"] as? Boolean ?: false,
                turnOrder = map["turnOrder"] as? List<String> ?: emptyList(),
                startedAt = (map["startedAt"] as? Number)?.toLong() ?: 0
            )
        }
    }
}

/**
 * 猜词消息
 */
data class GuessMessage(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val userName: String = "",
    val message: String = "",
    val isCorrect: Boolean = false,
    val isSystemMessage: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 游戏题目库
 */
object WordBank {
    val categories = mapOf(
        "动物" to listOf(
            "猫", "狗", "大象", "长颈鹿", "企鹅", "狮子", "老虎", "熊猫", 
            "兔子", "猴子", "蛇", "鳄鱼", "老鹰", "蝴蝶", "蜘蛛", "鲨鱼"
        ),
        "食物" to listOf(
            "披萨", "汉堡", "寿司", "冰淇淋", "蛋糕", "苹果", "香蕉", "西瓜",
            "面条", "饺子", "包子", "炸鸡", "薯条", "三明治", "巧克力", "咖啡"
        ),
        "物品" to listOf(
            "手机", "电脑", "汽车", "飞机", "自行车", "书本", "眼镜", "雨伞",
            "钥匙", "钟表", "电视", "沙发", "床", "桌子", "椅子", "灯泡"
        ),
        "动作" to listOf(
            "跑步", "跳舞", "唱歌", "游泳", "睡觉", "吃饭", "喝水", "看书",
            "打电话", "拍照", "开车", "骑车", "弹钢琴", "踢足球", "打篮球", "滑雪"
        ),
        "地点" to listOf(
            "学校", "医院", "公园", "海滩", "山", "图书馆", "超市", "餐厅",
            "电影院", "机场", "火车站", "银行", "教堂", "博物馆", "动物园", "游乐园"
        ),
        "职业" to listOf(
            "医生", "老师", "警察", "消防员", "厨师", "程序员", "画家", "歌手",
            "演员", "律师", "记者", "飞行员", "服务员", "科学家", "农民", "司机"
        )
    )
    
    /**
     * 随机获取一个词
     */
    fun getRandomWord(): Pair<String, String> {
        val category = categories.keys.random()
        val word = categories[category]?.random() ?: "猫"
        return category to word
    }
    
    /**
     * 获取提示（显示部分字母）
     */
    fun getHint(word: String): String {
        if (word.length <= 2) return "_ ".repeat(word.length).trim()
        val revealCount = word.length / 3
        val indices = (word.indices).shuffled().take(revealCount)
        return word.mapIndexed { index, char ->
            if (index in indices) char else '_'
        }.joinToString(" ")
    }
}
