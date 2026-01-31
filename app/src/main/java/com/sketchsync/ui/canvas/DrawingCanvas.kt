package com.sketchsync.ui.canvas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.sketchsync.data.model.DrawPath
import com.sketchsync.data.model.DrawTool
import com.sketchsync.data.model.PathPoint
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 自定义绘图画布View
 * 支持多种绘图工具、实时同步和多用户光标显示
 */
class DrawingCanvas @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    // 绘图状态
    private var currentTool: DrawTool = DrawTool.BRUSH
    private var currentColor: Int = Color.BLACK
    private var currentStrokeWidth: Float = 8f
    private var isEraser: Boolean = false
    
    // 当前正在绘制的路径
    private var currentPath: Path = Path()
    private var currentPoints: MutableList<PathPoint> = mutableListOf()
    private var startX: Float = 0f
    private var startY: Float = 0f
    
    // 所有已完成的路径
    private val completedPaths: MutableList<DrawPathWithAndroidPath> = mutableListOf()
    
    // 撤销/重做栈
    private val undoStack: MutableList<DrawPathWithAndroidPath> = mutableListOf()
    private val redoStack: MutableList<DrawPathWithAndroidPath> = mutableListOf()
    
    // 其他用户的光标
    private val otherCursors: MutableMap<String, Pair<Float, Float>> = mutableMapOf()
    private val cursorPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    // 画笔
    private val paint = Paint().apply {
        isAntiAlias = true
        isDither = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    
    // 橡皮擦画笔
    private val eraserPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    
    // 预览画笔（形状预览）
    private val previewPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        alpha = 128
    }
    
    // 背景Bitmap用于橡皮擦
    private var canvasBitmap: Bitmap? = null
    private var bitmapCanvas: Canvas? = null
    
    // 平移和缩放
    private var translateX: Float = 0f
    private var translateY: Float = 0f
    private var scaleFactor: Float = 1f
    private val minScale = 0.5f
    private val maxScale = 3f
    
    // 平移手势记录
    private var lastPanX: Float = 0f
    private var lastPanY: Float = 0f
    private var isPanning: Boolean = false
    
    // 缩放手势检测器
    private val scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = scaleFactor.coerceIn(minScale, maxScale)
            invalidate()
            return true
        }
    })
    
    // 回调
    var onPathCompleted: ((DrawPath) -> Unit)? = null
    var onCursorMoved: ((Float, Float) -> Unit)? = null
    
    init {
        // 使用SOFTWARE层类型以支持橡皮擦的PorterDuff.Mode.CLEAR
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        setBackgroundColor(Color.WHITE)
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 只在第一次初始化时创建bitmap，使用固定大小确保跨设备同步
        if (canvasBitmap == null && w > 0 && h > 0) {
            // 使用固定的画布大小，确保所有设备使用相同坐标系
            val canvasSize = CANVAS_SIZE
            canvasBitmap = Bitmap.createBitmap(canvasSize, canvasSize, Bitmap.Config.ARGB_8888)
            bitmapCanvas = Canvas(canvasBitmap!!)
            bitmapCanvas?.drawColor(Color.WHITE)
            redrawAllPaths()
            
            // 初始偏移量，使画布居中
            translateX = (w - canvasSize) / 2f
            translateY = (h - canvasSize) / 2f
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // 应用平移和缩放变换
        canvas.save()
        canvas.translate(translateX, translateY)
        canvas.scale(scaleFactor, scaleFactor)
        
        // 绘制缓存的bitmap
        canvasBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        
        // 绘制当前正在绘制的路径
        if (currentPoints.isNotEmpty()) {
            when (currentTool) {
                DrawTool.BRUSH, DrawTool.ERASER -> {
                    configurePaint()
                    canvas.drawPath(currentPath, if (isEraser) eraserPaint else paint)
                }
                DrawTool.LINE -> {
                    if (currentPoints.size >= 2) {
                        val last = currentPoints.last()
                        configurePreviewPaint()
                        canvas.drawLine(startX, startY, last.x, last.y, previewPaint)
                    }
                }
                DrawTool.RECTANGLE -> {
                    if (currentPoints.size >= 2) {
                        val last = currentPoints.last()
                        configurePreviewPaint()
                        canvas.drawRect(
                            minOf(startX, last.x), minOf(startY, last.y),
                            maxOf(startX, last.x), maxOf(startY, last.y),
                            previewPaint
                        )
                    }
                }
                DrawTool.CIRCLE -> {
                    if (currentPoints.size >= 2) {
                        val last = currentPoints.last()
                        val radius = sqrt(
                            (last.x - startX) * (last.x - startX) +
                            (last.y - startY) * (last.y - startY)
                        )
                        configurePreviewPaint()
                        canvas.drawCircle(startX, startY, radius, previewPaint)
                    }
                }
                DrawTool.TEXT, DrawTool.PAN -> {
                    // 文字工具在ACTION_UP时处理，PAN不绘制
                }
            }
        }
        
        // 绘制其他用户的光标
        drawOtherCursors(canvas)
        
        canvas.restore()
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 始终处理缩放手势（双指缩放）
        scaleGestureDetector.onTouchEvent(event)
        
        // 如果正在缩放，不处理其他手势
        if (scaleGestureDetector.isInProgress) {
            return true
        }
        
        val x = event.x
        val y = event.y
        
        // 如果是平移工具，处理平移逻辑
        if (currentTool == DrawTool.PAN) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastPanX = x
                    lastPanY = y
                    isPanning = true
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isPanning) {
                        translateX += x - lastPanX
                        translateY += y - lastPanY
                        lastPanX = x
                        lastPanY = y
                        invalidate()
                    }
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isPanning = false
                    return true
                }
            }
            return false
        }
        
        // 将触摸坐标转换为画布坐标（考虑平移和缩放）
        val canvasX = (x - translateX) / scaleFactor
        val canvasY = (y - translateY) / scaleFactor
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStart(canvasX, canvasY)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                touchMove(canvasX, canvasY)
                onCursorMoved?.invoke(canvasX, canvasY)
                return true
            }
            MotionEvent.ACTION_UP -> {
                touchEnd(canvasX, canvasY)
                return true
            }
        }
        return false
    }
    
    private fun touchStart(x: Float, y: Float) {
        currentPoints.clear()
        currentPoints.add(PathPoint(x, y))
        startX = x
        startY = y
        
        when (currentTool) {
            DrawTool.BRUSH, DrawTool.ERASER -> {
                currentPath.reset()
                currentPath.moveTo(x, y)
            }
            else -> {
                // 形状工具只需要记录起点
            }
        }
        
        invalidate()
    }
    
    private fun touchMove(x: Float, y: Float) {
        val lastPoint = currentPoints.lastOrNull() ?: return
        
        // 只有移动距离足够大才添加新点（优化性能）
        val dx = abs(x - lastPoint.x)
        val dy = abs(y - lastPoint.y)
        
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            currentPoints.add(PathPoint(x, y))
            
            when (currentTool) {
                DrawTool.BRUSH, DrawTool.ERASER -> {
                    currentPath.quadTo(
                        lastPoint.x, lastPoint.y,
                        (x + lastPoint.x) / 2, (y + lastPoint.y) / 2
                    )
                }
                else -> {
                    // 形状工具只需要更新最后一个点
                    if (currentPoints.size > 2) {
                        currentPoints.removeAt(currentPoints.size - 2)
                    }
                }
            }
            
            invalidate()
        }
    }
    
    private fun touchEnd(x: Float, y: Float) {
        if (currentPoints.isEmpty()) return
        
        currentPoints.add(PathPoint(x, y))
        
        // 创建DrawPath对象
        val drawPath = DrawPath(
            points = currentPoints.toList(),
            color = currentColor,
            strokeWidth = currentStrokeWidth,
            tool = currentTool,
            isEraser = isEraser
        )
        
        // 绘制到bitmap上
        val androidPath = createAndroidPath(drawPath)
        drawPathToBitmap(drawPath, androidPath)
        
        // 添加到完成列表
        val pathWithAndroid = DrawPathWithAndroidPath(drawPath, androidPath)
        completedPaths.add(pathWithAndroid)
        undoStack.add(pathWithAndroid)
        redoStack.clear()
        
        // 重置当前路径
        currentPath.reset()
        currentPoints.clear()
        
        // 回调通知路径完成
        onPathCompleted?.invoke(drawPath)
        
        invalidate()
    }
    
    private fun createAndroidPath(drawPath: DrawPath): Path {
        val path = Path()
        
        when (drawPath.tool) {
            DrawTool.BRUSH, DrawTool.ERASER -> {
                if (drawPath.points.isNotEmpty()) {
                    path.moveTo(drawPath.points[0].x, drawPath.points[0].y)
                    
                    for (i in 1 until drawPath.points.size) {
                        val prev = drawPath.points[i - 1]
                        val curr = drawPath.points[i]
                        path.quadTo(prev.x, prev.y, (curr.x + prev.x) / 2, (curr.y + prev.y) / 2)
                    }
                }
            }
            DrawTool.LINE -> {
                if (drawPath.points.size >= 2) {
                    val start = drawPath.points.first()
                    val end = drawPath.points.last()
                    path.moveTo(start.x, start.y)
                    path.lineTo(end.x, end.y)
                }
            }
            DrawTool.RECTANGLE -> {
                if (drawPath.points.size >= 2) {
                    val start = drawPath.points.first()
                    val end = drawPath.points.last()
                    path.addRect(
                        minOf(start.x, end.x), minOf(start.y, end.y),
                        maxOf(start.x, end.x), maxOf(start.y, end.y),
                        Path.Direction.CW
                    )
                }
            }
            DrawTool.CIRCLE -> {
                if (drawPath.points.size >= 2) {
                    val start = drawPath.points.first()
                    val end = drawPath.points.last()
                    val radius = sqrt(
                        (end.x - start.x) * (end.x - start.x) +
                        (end.y - start.y) * (end.y - start.y)
                    )
                    path.addCircle(start.x, start.y, radius, Path.Direction.CW)
                }
            }
            DrawTool.TEXT, DrawTool.PAN -> {
                // 文字工具需要特殊处理，PAN不创建路径
            }
        }
        
        return path
    }
    
    private fun drawPathToBitmap(drawPath: DrawPath, path: Path) {
        bitmapCanvas?.let { canvas ->
            val pathPaint = if (drawPath.isEraser) {
                eraserPaint.apply { strokeWidth = drawPath.strokeWidth }
            } else {
                paint.apply {
                    color = drawPath.color
                    strokeWidth = drawPath.strokeWidth
                }
            }
            canvas.drawPath(path, pathPaint)
        }
    }
    
    private fun redrawAllPaths() {
        canvasBitmap?.eraseColor(Color.WHITE)
        completedPaths.forEach { pathWithAndroid ->
            drawPathToBitmap(pathWithAndroid.drawPath, pathWithAndroid.androidPath)
        }
    }
    
    private fun configurePaint() {
        paint.color = currentColor
        paint.strokeWidth = currentStrokeWidth
        eraserPaint.strokeWidth = currentStrokeWidth
    }
    
    private fun configurePreviewPaint() {
        previewPaint.color = currentColor
        previewPaint.strokeWidth = currentStrokeWidth
    }
    
    private fun drawOtherCursors(canvas: Canvas) {
        otherCursors.forEach { (userId, position) ->
            val (x, y) = position
            cursorPaint.color = getUserColor(userId)
            canvas.drawCircle(x, y, 8f, cursorPaint)
        }
    }
    
    private fun getUserColor(userId: String): Int {
        val colors = listOf(
            Color.parseColor("#FF6B6B"),
            Color.parseColor("#4ECDC4"),
            Color.parseColor("#45B7D1"),
            Color.parseColor("#96CEB4"),
            Color.parseColor("#FFEAA7"),
            Color.parseColor("#DDA0DD"),
            Color.parseColor("#98D8C8")
        )
        return colors[abs(userId.hashCode()) % colors.size]
    }
    
    // ==================== 公共API ====================
    
    /**
     * 设置绘图工具
     */
    fun setTool(tool: DrawTool) {
        currentTool = tool
        isEraser = tool == DrawTool.ERASER
    }
    
    /**
     * 重置平移和缩放，使画布居中显示
     */
    fun resetPanZoom() {
        scaleFactor = 1f
        // 居中显示画布
        translateX = (width - CANVAS_SIZE) / 2f
        translateY = (height - CANVAS_SIZE) / 2f
        invalidate()
    }
    
    /**
     * 将坐标限制在画布范围内
     */
    private fun clampToCanvas(value: Float): Float {
        return value.coerceIn(0f, CANVAS_SIZE.toFloat())
    }
    
    /**
     * 设置画笔颜色
     */
    fun setColor(color: Int) {
        currentColor = color
    }
    
    /**
     * 设置画笔粗细
     */
    fun setStrokeWidth(width: Float) {
        currentStrokeWidth = width
    }
    
    /**
     * 撤销
     */
    fun undo(): Boolean {
        if (undoStack.isEmpty()) return false
        
        val path = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(path)
        completedPaths.remove(path)
        
        redrawAllPaths()
        invalidate()
        return true
    }
    
    /**
     * 重做
     */
    fun redo(): Boolean {
        if (redoStack.isEmpty()) return false
        
        val path = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(path)
        completedPaths.add(path)
        
        drawPathToBitmap(path.drawPath, path.androidPath)
        invalidate()
        return true
    }
    
    /**
     * 清空画布
     */
    fun clear() {
        completedPaths.clear()
        undoStack.clear()
        redoStack.clear()
        currentPath.reset()
        currentPoints.clear()
        canvasBitmap?.eraseColor(Color.WHITE)
        invalidate()
    }
    
    /**
     * 添加远程路径（来自其他用户）
     */
    fun addRemotePath(drawPath: DrawPath) {
        val androidPath = createAndroidPath(drawPath)
        val pathWithAndroid = DrawPathWithAndroidPath(drawPath, androidPath)
        completedPaths.add(pathWithAndroid)
        drawPathToBitmap(drawPath, androidPath)
        invalidate()
    }
    
    /**
     * 更新其他用户的光标位置
     */
    fun updateCursor(userId: String, x: Float, y: Float) {
        otherCursors[userId] = Pair(x, y)
        invalidate()
    }
    
    /**
     * 移除用户光标
     */
    fun removeCursor(userId: String) {
        otherCursors.remove(userId)
        invalidate()
    }
    
    /**
     * 导出画布为Bitmap
     */
    fun exportToBitmap(): Bitmap? {
        return canvasBitmap?.copy(Bitmap.Config.ARGB_8888, false)
    }
    
    /**
     * 导出画布为字节数组
     */
    fun exportToBytes(): ByteArray? {
        return canvasBitmap?.let { bitmap ->
            val stream = java.io.ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.toByteArray()
        }
    }
    
    /**
     * 获取当前路径数量
     */
    fun getPathCount(): Int = completedPaths.size
    
    /**
     * 是否可以撤销
     */
    fun canUndo(): Boolean = undoStack.isNotEmpty()
    
    /**
     * 是否可以重做
     */
    fun canRedo(): Boolean = redoStack.isNotEmpty()
    
    // ==================== 内部类 ====================
    
    private data class DrawPathWithAndroidPath(
        val drawPath: DrawPath,
        val androidPath: Path
    )
    
    companion object {
        // 固定画布大小，确保所有设备使用相同坐标系
        const val CANVAS_SIZE = 2000
        private const val TOUCH_TOLERANCE = 4f
    }
}
