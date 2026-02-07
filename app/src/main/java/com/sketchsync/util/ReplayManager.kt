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
    
    // 回放控制参数
    private var startIndex = 0
    private var totalDuration = 0L
    private var currentDuration = 0L
    private val frameRate = 16L // ~60fps
    private var startTime = 0L
    
    /**
     * 准备回放
     */
    fun prepare(paths: List<DrawPath>) {
        // 按时间戳排序
        allPaths = paths.sortedBy { it.timestamp }
        
        if (allPaths.isEmpty()) {
            _replayState.value = ReplayState.IDLE
            _progress.value = 0f
            _currentPathIds.value = emptySet()
            return
        }
        
        // 计算总时长
        val firstTimestamp = allPaths.first().timestamp
        val lastTimestamp = allPaths.last().timestamp
        totalDuration = lastTimestamp - firstTimestamp
        // 如果时长太短，至少保证有1秒
        if (totalDuration < 1000) totalDuration = 1000
            
        startIndex = 0
        currentDuration = 0
        
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
        val firstTimestamp = allPaths.first().timestamp
        
        replayJob = scope.launch(Dispatchers.Default) {
            val startSystemTime = System.currentTimeMillis()
            val startReplayTime = currentDuration
            
            while (currentDuration < totalDuration) {
                // 计算当前应该播放到的时间点
                val elapsedSystemTime = System.currentTimeMillis() - startSystemTime
                currentDuration = startReplayTime + (elapsedSystemTime * speed).toLong()
                
                if (currentDuration > totalDuration) currentDuration = totalDuration
                
                // 更新进度
                _progress.value = currentDuration.toFloat() / totalDuration
                
                // 找出当前时间点之前的所有路径
                val currentTimeThreshold = firstTimestamp + currentDuration
                val pathsToShow = allPaths.filter { it.timestamp <= currentTimeThreshold }
                val pathIds = pathsToShow.map { it.id }.toSet()
                _currentPathIds.value = pathIds
                
                if (currentDuration >= totalDuration) {
                    _replayState.value = ReplayState.COMPLETED
                    // 确保显示所有路径
                    _currentPathIds.value = allPaths.map { it.id }.toSet()
                    break
                }
                
                delay(frameRate)
            }
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
        currentDuration = 0
        startIndex = 0
    }
    
    /**
     * 跳转到指定进度
     */
    fun seekTo(progress: Float) {
        if (allPaths.isEmpty()) return
        
        val targetProgress = progress.coerceIn(0f, 1f)
        _progress.value = targetProgress
        currentDuration = (totalDuration * targetProgress).toLong()
        
        val firstTimestamp = allPaths.first().timestamp
        val currentTimeThreshold = firstTimestamp + currentDuration
        val pathsToShow = allPaths.filter { it.timestamp <= currentTimeThreshold }
        
        _currentPathIds.value = pathsToShow.map { it.id }.toSet()
        
        if (_replayState.value == ReplayState.PLAYING) {
            replayJob?.cancel()
            _replayState.value = ReplayState.PAUSED
            replayScope?.let { play(it, currentSpeed) }
        }

        if (_replayState.value == ReplayState.COMPLETED && targetProgress < 1f) {
            _replayState.value = ReplayState.PAUSED
        }
    }
}
