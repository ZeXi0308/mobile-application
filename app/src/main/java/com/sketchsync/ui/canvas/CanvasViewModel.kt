package com.sketchsync.ui.canvas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sketchsync.data.model.DrawPath
import com.sketchsync.data.model.DrawTool
import com.sketchsync.data.model.Room
import com.sketchsync.data.repository.AuthRepository
import com.sketchsync.data.repository.DrawingRepository
import com.sketchsync.data.repository.RoomRepository
import com.sketchsync.util.VoiceChatManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 画布界面ViewModel
 * 管理绘图状态、实时同步和语音聊天
 */
@HiltViewModel
class CanvasViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val roomRepository: RoomRepository,
    private val drawingRepository: DrawingRepository,
    private val voiceChatManager: VoiceChatManager
) : ViewModel() {
    
    // UI状态
    private val _uiState = MutableStateFlow(CanvasUiState())
    val uiState: StateFlow<CanvasUiState> = _uiState.asStateFlow()
    
    // 房间信息
    private val _room = MutableStateFlow<Room?>(null)
    val room: StateFlow<Room?> = _room.asStateFlow()
    
    // 远程绘图路径
    private val _remotePaths = MutableStateFlow<List<DrawPath>>(emptyList())
    val remotePaths: StateFlow<List<DrawPath>> = _remotePaths.asStateFlow()
    
    // 其他用户光标
    private val _cursors = MutableStateFlow<Map<String, Pair<Float, Float>>>(emptyMap())
    val cursors: StateFlow<Map<String, Pair<Float, Float>>> = _cursors.asStateFlow()
    
    // 清空事件（其他用户清空画布时触发）
    private val _clearEvent = MutableStateFlow(0L)
    val clearEvent: StateFlow<Long> = _clearEvent.asStateFlow()
    
    // 当前用户信息
    val currentUserId: String? get() = authRepository.currentUserId
    
    private var currentRoomId: String? = null
    
    /**
     * 加入房间并开始同步
     */
    fun joinRoom(roomId: String) {
        currentRoomId = roomId
        
        viewModelScope.launch {
            // 获取房间信息
            roomRepository.getRoom(roomId).onSuccess { room ->
                _room.value = room
            }
            
            // 加载已有路径
            loadExistingPaths(roomId)
            
            // 开始监听新路径
            startPathSync(roomId)
            
            // 开始监听光标
            startCursorSync(roomId)
            
            // 开始监听清空事件
            startClearEventSync(roomId)
        }
    }
    
    /**
     * 加载已有的绘图路径
     */
    private suspend fun loadExistingPaths(roomId: String) {
        drawingRepository.getAllPaths(roomId).onSuccess { paths ->
            _remotePaths.value = paths
        }
    }
    
    /**
     * 开始路径同步
     */
    private fun startPathSync(roomId: String) {
        viewModelScope.launch {
            drawingRepository.observeDrawPaths(roomId)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
                .collect { path ->
                    // 过滤掉自己发送的路径
                    if (path.userId != currentUserId) {
                        _remotePaths.value = _remotePaths.value + path
                    }
                }
        }
    }
    
    /**
     * 开始光标同步
     */
    private fun startCursorSync(roomId: String) {
        viewModelScope.launch {
            currentUserId?.let { userId ->
                drawingRepository.observeCursors(roomId, userId)
                    .catch { /* 忽略错误 */ }
                    .collect { cursors ->
                        _cursors.value = cursors
                    }
            }
        }
    }
    
    /**
     * 开始监听清空事件
     */
    private fun startClearEventSync(roomId: String) {
        viewModelScope.launch {
            drawingRepository.observeClearEvents(roomId)
                .catch { /* 忽略错误 */ }
                .collect { timestamp ->
                    _clearEvent.value = timestamp
                    _remotePaths.value = emptyList()
                }
        }
    }
    
    /**
     * 发送绘图路径
     */
    fun sendPath(path: DrawPath) {
        val roomId = currentRoomId ?: return
        val userId = currentUserId ?: return
        
        viewModelScope.launch {
            val userProfile = authRepository.getCurrentUserProfile().getOrNull()
            val pathWithUser = path.copy(
                userId = userId,
                userName = userProfile?.displayName ?: "用户"
            )
            
            drawingRepository.sendDrawPath(roomId, pathWithUser)
        }
    }
    
    /**
     * 更新光标位置
     */
    fun updateCursor(x: Float, y: Float) {
        val roomId = currentRoomId ?: return
        val userId = currentUserId ?: return
        
        drawingRepository.updateCursorPosition(roomId, userId, x, y)
    }
    
    /**
     * 清空画布
     */
    fun clearCanvas() {
        val roomId = currentRoomId ?: return
        
        viewModelScope.launch {
            drawingRepository.clearCanvas(roomId).onSuccess {
                _remotePaths.value = emptyList()
            }
        }
    }
    
    /**
     * 加入语音频道
     */
    fun joinVoiceChannel() {
        val roomId = currentRoomId ?: return
        val userId = currentUserId ?: return
        
        _uiState.value = _uiState.value.copy(isVoiceConnecting = true)
        
        // 设置监听器
        voiceChatManager.setOnJoinChannelSuccessListener { channel, uid ->
            _uiState.value = _uiState.value.copy(
                isVoiceEnabled = true,
                isVoiceConnecting = false,
                message = "语音聊天已连接"
            )
        }
        
        voiceChatManager.setOnErrorListener { err, msg ->
            _uiState.value = _uiState.value.copy(
                isVoiceEnabled = false,
                isVoiceConnecting = false,
                error = "语音错误: $msg ($err)"
            )
        }
        
        val success = voiceChatManager.joinChannel(roomId, userId.hashCode())
        if (!success) {
            _uiState.value = _uiState.value.copy(
                isVoiceConnecting = false,
                error = "无法初始化语音引擎"
            )
        }
    }
    
    /**
     * 离开语音频道
     */
    fun leaveVoiceChannel() {
        voiceChatManager.leaveChannel()
        _uiState.value = _uiState.value.copy(isVoiceEnabled = false)
    }
    
    /**
     * 切换静音
     */
    fun toggleMute() {
        val isMuted = voiceChatManager.toggleMute()
        _uiState.value = _uiState.value.copy(isMuted = isMuted)
    }
    
    /**
     * 设置工具
     */
    fun setTool(tool: DrawTool) {
        _uiState.value = _uiState.value.copy(currentTool = tool)
    }
    
    /**
     * 设置颜色
     */
    fun setColor(color: Int) {
        _uiState.value = _uiState.value.copy(currentColor = color)
    }
    
    /**
     * 设置画笔粗细
     */
    fun setStrokeWidth(width: Float) {
        _uiState.value = _uiState.value.copy(currentStrokeWidth = width)
    }
    
    /**
     * 导出画布
     */
    fun exportCanvas(imageBytes: ByteArray?) {
        val roomId = currentRoomId ?: return
        
        if (imageBytes == null || imageBytes.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "画布为空，无法保存")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, message = "正在保存...")
            
            drawingRepository.uploadCanvasImage(roomId, imageBytes)
                .onSuccess { url ->
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        exportedImageUrl = url,
                        message = "图片保存成功"
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        error = "保存失败: ${e.message}"
                    )
                }
        }
    }
    
    /**
     * 离开房间
     */
    fun leaveRoom() {
        val roomId = currentRoomId ?: return
        val userId = currentUserId ?: return
        
        viewModelScope.launch {
            roomRepository.leaveRoom(roomId, userId)
            voiceChatManager.leaveChannel()
        }
    }
    
    /**
     * 清除消息
     */
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }
    
    override fun onCleared() {
        super.onCleared()
        leaveRoom()
    }
}

/**
 * 画布UI状态
 */
data class CanvasUiState(
    val currentTool: DrawTool = DrawTool.BRUSH,
    val currentColor: Int = android.graphics.Color.BLACK,
    val currentStrokeWidth: Float = 8f,
    val isVoiceEnabled: Boolean = false,
    val isVoiceConnecting: Boolean = false,
    val isMuted: Boolean = false,
    val isExporting: Boolean = false,
    val exportedImageUrl: String? = null,
    val message: String? = null,
    val error: String? = null
)
