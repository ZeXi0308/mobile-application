package com.sketchsync.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sketchsync.data.model.ChatMessage
import com.sketchsync.data.model.MessageType
import com.sketchsync.ui.theme.PrimaryBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * 聊天面板组件
 * 可以嵌入到CanvasScreen中使用
 */
@Composable
fun ChatPanel(
    messages: List<ChatMessage>,
    currentUserId: String,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    isGameMode: Boolean = false,  // 游戏模式下用于猜词
    hintText: String = "输入消息..."
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    // 自动滚动到最新消息
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        // 消息列表
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                ChatMessageItem(
                    message = message,
                    isOwnMessage = message.userId == currentUserId
                )
            }
        }
        
        // 输入框
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(hintText) },
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue)
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "发送",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

/**
 * 单条消息组件
 */
@Composable
fun ChatMessageItem(
    message: ChatMessage,
    isOwnMessage: Boolean
) {
    when (message.type) {
        MessageType.SYSTEM -> {
            // 系统消息（居中显示）
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = message.content,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .background(
                            Color.LightGray.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
        
        MessageType.CORRECT_GUESS -> {
            // 猜对了的消息（特殊样式）
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🎉 ${message.userName} 猜对了！",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .background(
                            Color(0xFF4CAF50).copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
        
        else -> {
            // 普通消息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
            ) {
                if (!isOwnMessage) {
                    // 头像
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(getUserColor(message.userId)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = message.userName.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                Column(
                    horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
                ) {
                    if (!isOwnMessage) {
                        Text(
                            text = message.userName,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    
                    // 消息气泡
                    Box(
                        modifier = Modifier
                            .widthIn(max = 240.dp)
                            .background(
                                if (isOwnMessage) PrimaryBlue else Color.LightGray.copy(alpha = 0.3f),
                                RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isOwnMessage) 16.dp else 4.dp,
                                    bottomEnd = if (isOwnMessage) 4.dp else 16.dp
                                )
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = message.content,
                            color = if (isOwnMessage) Color.White else Color.Black
                        )
                    }
                    
                    // 时间
                    Text(
                        text = formatTime(message.timestamp),
                        fontSize = 10.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * 获取用户颜色（根据用户ID生成固定颜色）
 */
private fun getUserColor(userId: String): Color {
    val colors = listOf(
        Color(0xFFFF6B6B),
        Color(0xFF4ECDC4),
        Color(0xFF45B7D1),
        Color(0xFF96CEB4),
        Color(0xFFDDA0DD),
        Color(0xFF98D8C8)
    )
    return colors[abs(userId.hashCode()) % colors.size]
}

/**
 * 格式化时间
 */
private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
