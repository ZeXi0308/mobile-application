package com.sketchsync.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sketchsync.data.repository.AuthRepository
import com.sketchsync.data.repository.DrawingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 画廊ViewModel
 */
@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val drawingRepository: DrawingRepository
) : ViewModel() {
    
    private val _images = MutableStateFlow<List<GalleryImage>>(emptyList())
    val images: StateFlow<List<GalleryImage>> = _images.asStateFlow()
    
    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()
    
    /**
     * 加载用户保存的图片
     */
    fun loadImages() {
        val userId = authRepository.currentUserId ?: return
        
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        viewModelScope.launch {
            drawingRepository.getSavedImages(userId)
                .onSuccess { urls ->
                    // 将URL列表转换为GalleryImage列表
                    val galleryImages = urls.mapIndexed { index, url ->
                        GalleryImage(
                            id = "img_$index",
                            url = url,
                            roomId = "",
                            roomName = "画作 ${index + 1}",
                            createdAt = System.currentTimeMillis() - (index * 86400000L)
                        )
                    }
                    _images.value = galleryImages
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
        }
    }
    
    /**
     * 删除图片
     */
    fun deleteImage(image: GalleryImage) {
        viewModelScope.launch {
            // TODO: 实现删除逻辑
            _images.value = _images.value.filter { it.id != image.id }
        }
    }
}

/**
 * 画廊UI状态
 */
data class GalleryUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
