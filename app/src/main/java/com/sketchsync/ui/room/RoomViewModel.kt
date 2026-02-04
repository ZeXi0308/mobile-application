package com.sketchsync.ui.room

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sketchsync.data.model.GameMode
import com.sketchsync.data.model.Room
import com.sketchsync.data.repository.AuthRepository
import com.sketchsync.data.repository.RoomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "RoomViewModel"

/**
 * 房间列表ViewModel
 * 管理房间创建、加入和列表显示
 */
@HiltViewModel
class RoomViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val roomRepository: RoomRepository
) : ViewModel() {
    
    // 房间列表
    private val _rooms = MutableStateFlow<List<Room>>(emptyList())
    val rooms: StateFlow<List<Room>> = _rooms.asStateFlow()
    
    // 每个房间的实时在线人数（使用Presence系统）
    private val _roomPresenceCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val roomPresenceCounts: StateFlow<Map<String, Int>> = _roomPresenceCounts.asStateFlow()
    
    // UI状态
    private val _uiState = MutableStateFlow(RoomUiState())
    val uiState: StateFlow<RoomUiState> = _uiState.asStateFlow()
    
    // 当前用户信息
    val currentUserId: String? get() = authRepository.currentUserId
    
    init {
        loadRooms()
    }
    
    /**
     * 加载房间列表
     */
    private fun loadRooms() {
        Log.d(TAG, "loadRooms: Starting rooms and presence observers")
        
        viewModelScope.launch {
            roomRepository.observeActiveRooms()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
                .collect { rooms ->
                    Log.d(TAG, "loadRooms: Received ${rooms.size} rooms")
                    _rooms.value = rooms
                }
        }
        
        // 同时观察所有房间的实时在线人数
        viewModelScope.launch {
            roomRepository.observeAllRoomPresenceCounts()
                .catch { e -> 
                    Log.e(TAG, "loadRooms: Presence observation error: ${e.message}")
                }
                .collect { counts ->
                    Log.d(TAG, "loadRooms: Received presence counts: $counts")
                    _roomPresenceCounts.value = counts
                }
        }
    }
    
    /**
     * 刷新房间列表
     */
    fun refresh() {
        loadRooms()
    }
    
    /**
     * 创建房间
     */
    fun createRoom(
        name: String,
        maxParticipants: Int = 8,
        isPrivate: Boolean = false,
        password: String? = null,
        gameMode: GameMode = GameMode.FREE_DRAW
    ) {
        val userId = currentUserId
        if (userId == null) {
            _uiState.value = _uiState.value.copy(error = "请先登录")
            return
        }
        
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "请输入房间名称")
            return
        }
        
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        viewModelScope.launch {
            val userProfile = authRepository.getCurrentUserProfile().getOrNull()
            val userName = userProfile?.displayName ?: "用户${userId.take(6)}"
            
            roomRepository.createRoom(
                name = name,
                creatorId = userId,
                creatorName = userName,
                maxParticipants = maxParticipants,
                isPrivate = isPrivate,
                password = password,
                gameMode = gameMode
            ).onSuccess { room ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    createdRoomId = room.id,
                    message = "房间创建成功"
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "创建失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 加入房间
     */
    fun joinRoom(roomId: String, password: String? = null) {
        val userId = currentUserId
        if (userId == null) {
            _uiState.value = _uiState.value.copy(error = "请先登录")
            return
        }
        
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        viewModelScope.launch {
            val userProfile = authRepository.getCurrentUserProfile().getOrNull()
            val userName = userProfile?.displayName ?: "用户${userId.take(6)}"
            
            roomRepository.joinRoom(roomId, userId, userName, password)
                .onSuccess { room ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        joinedRoomId = room.id
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "加入失败"
                    )
                }
        }
    }
    
    /**
     * 通过ID加入房间
     */
    fun joinRoomById(roomId: String) {
        if (roomId.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "请输入房间ID")
            return
        }
        
        joinRoom(roomId.trim())
    }
    
    /**
     * 搜索房间
     */
    fun searchRooms(query: String) {
        if (query.isBlank()) {
            loadRooms()
            return
        }
        
        viewModelScope.launch {
            roomRepository.searchRooms(query)
                .onSuccess { results ->
                    _rooms.value = results
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = "搜索失败: ${e.message}")
                }
        }
    }
    
    /**
     * 清除创建的房间ID
     */
    fun clearCreatedRoomId() {
        _uiState.value = _uiState.value.copy(createdRoomId = null)
    }
    
    /**
     * 清除加入的房间ID
     */
    fun clearJoinedRoomId() {
        _uiState.value = _uiState.value.copy(joinedRoomId = null)
    }
    
    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * 清除消息
     */
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}

/**
 * 房间UI状态
 */
data class RoomUiState(
    val isLoading: Boolean = false,
    val createdRoomId: String? = null,
    val joinedRoomId: String? = null,
    val message: String? = null,
    val error: String? = null
)
