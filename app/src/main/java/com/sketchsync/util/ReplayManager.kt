package com.sketchsync.util

import com.sketchsync.data.model.DrawPath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * 回放状态
 */
enum class ReplayState {
    IDLE,       // 空闲
    PLAYING,    // 播放中
    PAUSED,     // 暂停
    COMPLETED   // 完成
}

/**
 * 绘画过程回放管理器
 */
@Singleton
class ReplayManager @Inject constructor() {
    
    // 回放状态
    private val _replayState = MutableStateFlow(ReplayState.IDLE)
    val replayState: StateFlow<ReplayState> = _replayState.asStateFlow()
    
    // 当前进度 (0f - 1f)
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()
    
    // 当前回放到的路径ID列表
    private val _currentPathIds = MutableStateFlow<Set<String>>(emptySet())
    val currentPathIds: StateFlow<Set<String>> = _currentPathIds.asStateFlow()
    
    // 所有需要回放的路径
    private var allPaths: List<DrawPath> = emptyList()
    
    // 回放Job
    private var replayJob: Job? = null
    private var replayScope: CoroutineScope? = null
    private var currentSpeed = 1.0f
    
    // 回放控制参数（按笔画顺序）
    private var currentIndex = 0
    private var totalCount = 0
    private val stepIntervalMs = 120L
    
    /**
     * 准备回放
     */
    fun prepare(paths: List<DrawPath>) {
        // 按笔画顺序回放（不使用时间轴）
        allPaths = paths
        
        if (allPaths.isEmpty()) {
            _replayState.value = ReplayState.IDLE
            _progress.value = 0f
            _currentPathIds.value = emptySet()
            return
        }
        
        totalCount = allPaths.size
        currentIndex = 0
        
        _replayState.value = ReplayState.PAUSED
        _progress.value = 0f
        _currentPathIds.value = emptySet()
    }
    
    /**
     * 开始/继续回放
     * @param scope 协程作用域
     * @param speed 倍速 (1.0 = 正常速度)
     */
    fun play(scope: CoroutineScope, speed: Float = 1.0f) {
        if (allPaths.isEmpty() || _replayState.value == ReplayState.PLAYING) return
        
        replayScope = scope
        currentSpeed = speed
        _replayState.value = ReplayState.PLAYING

        if (totalCount <= 1) {
            _currentPathIds.value = allPaths.map { it.id }.toSet()
            _progress.value = 1f
            _replayState.value = ReplayState.COMPLETED
            return
        }

        if (currentIndex >= totalCount) {
            currentIndex = 0
            _currentPathIds.value = emptySet()
            _progress.value = 0f
        } else {
            updateReplayFrame()
        }

        replayJob = scope.launch(Dispatchers.Default) {
            while (currentIndex < totalCount - 1) {
                delay((stepIntervalMs / speed).toLong().coerceAtLeast(16L))
                currentIndex++
                updateReplayFrame()
            }
            _replayState.value = ReplayState.COMPLETED
            _currentPathIds.value = allPaths.map { it.id }.toSet()
            _progress.value = 1f
        }
    }
    
    /**
     * 暂停回放
     */
    fun pause() {
        if (_replayState.value != ReplayState.PLAYING) return
        
        replayJob?.cancel()
        _replayState.value = ReplayState.PAUSED
    }
    
    /**
     * 停止/重置回放
     */
    fun stop() {
        replayJob?.cancel()
        _replayState.value = ReplayState.IDLE
        _progress.value = 0f
        _currentPathIds.value = emptySet() // IDLE状态下不显示回放路径（由原来的逻辑接管）
        currentIndex = 0
    }
    
    /**
     * 跳转到指定进度
     */
    fun seekTo(progress: Float) {
        if (allPaths.isEmpty()) return
        
        val targetProgress = progress.coerceIn(0f, 1f)
        currentIndex = ((totalCount - 1) * targetProgress).roundToInt().coerceIn(0, totalCount - 1)
        updateReplayFrame()
        
        if (_replayState.value == ReplayState.PLAYING) {
            replayJob?.cancel()
            _replayState.value = ReplayState.PAUSED
            replayScope?.let { play(it, currentSpeed) }
        }

        if (_replayState.value == ReplayState.COMPLETED && targetProgress < 1f) {
            _replayState.value = ReplayState.PAUSED
        }
    }

    private fun updateReplayFrame() {
        val count = totalCount
        if (count <= 0) {
            _progress.value = 0f
            _currentPathIds.value = emptySet()
            return
        }
        _progress.value = if (count <= 1) 1f else currentIndex.toFloat() / (count - 1).toFloat()
        _currentPathIds.value = allPaths.take(currentIndex + 1).map { it.id }.toSet()
    }
}
