package com.sketchsync.data.model

import android.graphics.Color
import java.util.UUID

/**
 * 绘图路径数据模型
 * 用于表示用户在画布上绘制的一条路径
 */
data class DrawPath(
    val id: String = UUID.randomUUID().toString(),
    val odatabaseId: String = "", // Firebase中的路径ID
    val points: List<PathPoint> = emptyList(),
    val color: Int = Color.BLACK,
    val strokeWidth: Float = 5f,
    val tool: DrawTool = DrawTool.BRUSH,
    val userId: String = "",
    val userName: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isEraser: Boolean = false
) {
    /**
     * 转换为Firebase可存储的Map格式
     */
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "points" to points.map { mapOf("x" to it.x, "y" to it.y) },
        "color" to color,
        "strokeWidth" to strokeWidth,
        "tool" to tool.name,
        "userId" to userId,
        "userName" to userName,
        "timestamp" to timestamp,
        "isEraser" to isEraser
    )
    
    companion object {
        /**
         * 从Firebase Map数据创建DrawPath对象
         */
        fun fromMap(map: Map<String, Any?>, pathId: String = ""): DrawPath {
            @Suppress("UNCHECKED_CAST")
            val pointsList = (map["points"] as? List<Map<String, Double>>)?.map { p ->
                PathPoint(
                    x = (p["x"] as? Number)?.toFloat() ?: 0f,
                    y = (p["y"] as? Number)?.toFloat() ?: 0f
                )
            } ?: emptyList()
            
            return DrawPath(
                id = map["id"] as? String ?: pathId,
                odatabaseId = pathId,
                points = pointsList,
                color = (map["color"] as? Number)?.toInt() ?: Color.BLACK,
                strokeWidth = (map["strokeWidth"] as? Number)?.toFloat() ?: 5f,
                tool = try { 
                    DrawTool.valueOf(map["tool"] as? String ?: "BRUSH") 
                } catch (e: Exception) { 
                    DrawTool.BRUSH 
                },
                userId = map["userId"] as? String ?: "",
                userName = map["userName"] as? String ?: "",
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                isEraser = map["isEraser"] as? Boolean ?: false
            )
        }
    }
}

/**
 * 路径上的点
 */
data class PathPoint(
    val x: Float = 0f,
    val y: Float = 0f
)

/**
 * 绘图工具类型
 */
enum class DrawTool {
    BRUSH,      // 画笔
    ERASER,     // 橡皮擦
    LINE,       // 直线
    RECTANGLE,  // 矩形
    CIRCLE,     // 圆形
    TEXT,       // 文字
    PAN         // 拖拽/平移
}
