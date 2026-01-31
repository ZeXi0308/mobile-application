package com.sketchsync.util

import android.content.Context
import android.util.Log
import com.sketchsync.BuildConfig
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 语音聊天管理器
 * 使用Agora SDK实现实时语音通话
 */
@Singleton
class VoiceChatManager @Inject constructor(
    private val context: Context
) {
    private var rtcEngine: RtcEngine? = null
    private var isInitialized = false
    private var isMuted = false
    private var currentChannel: String? = null
    
    private var onJoinChannelSuccessListener: ((String, Int) -> Unit)? = null
    private var onUserJoinedListener: ((Int) -> Unit)? = null
    private var onUserLeftListener: ((Int) -> Unit)? = null
    private var onErrorListener: ((Int, String) -> Unit)? = null
    
    private val eventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            Log.d(TAG, "Join channel success: $channel, uid: $uid")
            currentChannel = channel
            onJoinChannelSuccessListener?.invoke(channel, uid)
        }
        
        override fun onUserJoined(uid: Int, elapsed: Int) {
            Log.d(TAG, "User joined: $uid")
            onUserJoinedListener?.invoke(uid)
        }
        
        override fun onUserOffline(uid: Int, reason: Int) {
            Log.d(TAG, "User offline: $uid, reason: $reason")
            onUserLeftListener?.invoke(uid)
        }
        
        override fun onError(err: Int) {
            Log.e(TAG, "Agora error: $err")
            onErrorListener?.invoke(err, getErrorMessage(err))
        }
        
        override fun onLeaveChannel(stats: RtcStats?) {
            Log.d(TAG, "Left channel")
            currentChannel = null
        }
    }
    
    /**
     * 初始化Agora引擎
     */
    fun initialize(): Boolean {
        if (isInitialized) return true
        
        val appId = BuildConfig.AGORA_APP_ID
        if (appId.isBlank()) {
            Log.e(TAG, "Agora App ID is empty")
            return false
        }
        
        return try {
            val config = RtcEngineConfig().apply {
                mContext = context.applicationContext
                mAppId = appId
                mEventHandler = eventHandler
            }
            
            rtcEngine = RtcEngine.create(config)
            rtcEngine?.apply {
                enableAudio()
                setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
                setAudioProfile(
                    Constants.AUDIO_PROFILE_DEFAULT,
                    Constants.AUDIO_SCENARIO_CHATROOM
                )
            }
            
            isInitialized = true
            Log.d(TAG, "Agora initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Agora: ${e.message}")
            false
        }
    }
    
    /**
     * 加入语音频道
     */
    fun joinChannel(channelId: String, userId: Int): Boolean {
        if (!isInitialized) {
            if (!initialize()) return false
        }
        
        return try {
            val options = ChannelMediaOptions().apply {
                autoSubscribeAudio = true
                publishMicrophoneTrack = true
                clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            }
            
            val result = rtcEngine?.joinChannel(null, channelId, userId, options)
            result == 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to join channel: ${e.message}")
            false
        }
    }
    
    /**
     * 离开语音频道
     */
    fun leaveChannel() {
        try {
            rtcEngine?.leaveChannel()
            currentChannel = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to leave channel: ${e.message}")
        }
    }
    
    /**
     * 静音/取消静音
     */
    fun setMuted(muted: Boolean) {
        isMuted = muted
        rtcEngine?.muteLocalAudioStream(muted)
    }
    
    /**
     * 切换静音状态
     */
    fun toggleMute(): Boolean {
        isMuted = !isMuted
        rtcEngine?.muteLocalAudioStream(isMuted)
        return isMuted
    }
    
    /**
     * 获取当前静音状态
     */
    fun isMuted(): Boolean = isMuted
    
    /**
     * 是否已加入频道
     */
    fun isInChannel(): Boolean = currentChannel != null
    
    /**
     * 设置监听器
     */
    fun setOnJoinChannelSuccessListener(listener: (String, Int) -> Unit) {
        onJoinChannelSuccessListener = listener
    }

    fun setOnUserJoinedListener(listener: (Int) -> Unit) {
        onUserJoinedListener = listener
    }
    
    fun setOnUserLeftListener(listener: (Int) -> Unit) {
        onUserLeftListener = listener
    }
    
    fun setOnErrorListener(listener: (Int, String) -> Unit) {
        onErrorListener = listener
    }
    
    /**
     * 销毁引擎
     */
    fun destroy() {
        rtcEngine?.leaveChannel()
        RtcEngine.destroy()
        rtcEngine = null
        isInitialized = false
        currentChannel = null
    }
    
    private fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            Constants.ERR_INVALID_TOKEN -> "Token无效"
            Constants.ERR_TOKEN_EXPIRED -> "Token已过期"
            Constants.ERR_NOT_INITIALIZED -> "未初始化"
            Constants.ERR_CONNECTION_INTERRUPTED -> "连接中断"
            Constants.ERR_CONNECTION_LOST -> "连接丢失"
            Constants.ERR_NOT_IN_CHANNEL -> "不在频道中"
            else -> "未知错误: $errorCode"
        }
    }
    
    companion object {
        private const val TAG = "VoiceChatManager"
    }
}
