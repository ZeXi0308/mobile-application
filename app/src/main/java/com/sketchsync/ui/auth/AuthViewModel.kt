package com.sketchsync.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sketchsync.data.model.AuthState
import com.sketchsync.data.model.User
import com.sketchsync.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 认证ViewModel
 * 管理登录、注册和用户状态
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    // 认证状态
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    // UI状态
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    init {
        checkAuthState()
    }
    
    /**
     * 检查认证状态
     */
    private fun checkAuthState() {
        viewModelScope.launch {
            authRepository.observeAuthState().collect { firebaseUser ->
                if (firebaseUser != null) {
                    val profileResult = authRepository.getUserProfile(firebaseUser.uid)
                    if (profileResult.isSuccess) {
                        _authState.value = AuthState.Authenticated(profileResult.getOrThrow())
                    } else {
                        // 用户已验证但没有资料，创建默认资料
                        val user = User(
                            uid = firebaseUser.uid,
                            email = firebaseUser.email ?: "",
                            displayName = firebaseUser.displayName ?: "用户${firebaseUser.uid.take(6)}"
                        )
                        _authState.value = AuthState.Authenticated(user)
                    }
                } else {
                    _authState.value = AuthState.Unauthenticated
                }
            }
        }
    }
    
    /**
     * 邮箱登录
     */
    fun signInWithEmail(email: String, password: String) {
        if (!validateInput(email, password)) return
        
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        
        viewModelScope.launch {
            authRepository.signInWithEmail(email, password)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = mapFirebaseError(e)
                    )
                }
        }
    }
    
    /**
     * 邮箱注册
     */
    fun signUpWithEmail(email: String, password: String, displayName: String) {
        if (!validateInput(email, password, displayName)) return
        
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        
        viewModelScope.launch {
            authRepository.signUpWithEmail(email, password, displayName)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = mapFirebaseError(e)
                    )
                }
        }
    }
    
    /**
     * Google登录
     */
    fun signInWithGoogle(idToken: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        
        viewModelScope.launch {
            authRepository.signInWithGoogle(idToken)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Google登录失败: ${e.message}"
                    )
                }
        }
    }
    
    /**
     * 发送密码重置邮件
     */
    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "请输入邮箱地址")
            return
        }
        
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        viewModelScope.launch {
            authRepository.sendPasswordResetEmail(email)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "重置邮件已发送，请查收"
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = mapFirebaseError(e)
                    )
                }
        }
    }
    
    /**
     * 退出登录
     */
    fun signOut() {
        authRepository.signOut()
    }
    
    /**
     * 更新用户资料
     */
    fun updateProfile(displayName: String) {
        val userId = authRepository.currentUserId ?: return
        
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        viewModelScope.launch {
            authRepository.updateUserProfile(userId, displayName)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "资料更新成功"
                    )
                    // 刷新用户信息
                    checkAuthState()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "更新失败: ${e.message}"
                    )
                }
        }
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
    
    private fun validateInput(email: String, password: String, displayName: String? = null): Boolean {
        when {
            email.isBlank() -> {
                _uiState.value = _uiState.value.copy(error = "请输入邮箱")
                return false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                _uiState.value = _uiState.value.copy(error = "邮箱格式不正确")
                return false
            }
            password.isBlank() -> {
                _uiState.value = _uiState.value.copy(error = "请输入密码")
                return false
            }
            password.length < 6 -> {
                _uiState.value = _uiState.value.copy(error = "密码至少需要6个字符")
                return false
            }
            displayName != null && displayName.isBlank() -> {
                _uiState.value = _uiState.value.copy(error = "请输入用户名")
                return false
            }
        }
        return true
    }
    
    private fun mapFirebaseError(e: Throwable): String {
        val message = e.message ?: "未知错误"
        return when {
            message.contains("INVALID_EMAIL") -> "邮箱格式不正确"
            message.contains("WRONG_PASSWORD") || message.contains("INVALID_LOGIN_CREDENTIALS") -> "邮箱或密码错误"
            message.contains("USER_NOT_FOUND") -> "用户不存在"
            message.contains("EMAIL_EXISTS") || message.contains("EMAIL_ALREADY_IN_USE") -> "该邮箱已被注册"
            message.contains("WEAK_PASSWORD") -> "密码强度不够"
            message.contains("NETWORK") -> "网络连接失败，请检查网络"
            message.contains("TOO_MANY_REQUESTS") -> "请求过于频繁，请稍后再试"
            else -> message
        }
    }
}

/**
 * 认证UI状态
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)
