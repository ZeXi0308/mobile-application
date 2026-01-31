package com.sketchsync.ui.game

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.sketchsync.data.model.PictionaryGame
import com.sketchsync.ui.theme.PrimaryBlue
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * 游戏状态面板
 * 显示在画布顶部，包含计时器、当前词语、积分等
 */
@Composable
fun GameStatusPanel(
    game: PictionaryGame,
    currentUserId: String,
    isDrawer: Boolean,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = game.timeRemaining.toFloat() / game.totalTime.toFloat(),
        animationSpec = tween(1000),
        label = "timer"
    )
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDrawer) Color(0xFFE3F2FD) else Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // 轮次和计时器
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 轮次
                Text(
                    text = "第 ${game.round}/${game.maxRounds} 轮",
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
                
                // 计时器
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (game.timeRemaining <= 10) Color.Red else PrimaryBlue
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${game.timeRemaining}s",
                        fontWeight = FontWeight.Bold,
                        color = if (game.timeRemaining <= 10) Color.Red else PrimaryBlue,
                        fontSize = 18.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 时间进度条
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = when {
                    game.timeRemaining <= 10 -> Color.Red
                    game.timeRemaining <= 30 -> Color(0xFFFF9800)
                    else -> PrimaryBlue
                },
                trackColor = Color.LightGray.copy(alpha = 0.3f)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 词语显示
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isDrawer) PrimaryBlue else Color.LightGray.copy(alpha = 0.2f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isDrawer) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "你要画的词是",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = game.currentWord,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "猜这个词",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = game.wordHint,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 当前画家
            Text(
                text = if (isDrawer) "轮到你画了！" else "🎨 ${game.currentDrawerName} 正在画",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 积分榜组件
 */
@Composable
fun ScoreBoard(
    scores: Map<String, Int>,
    participantNames: Map<String, String>,
    currentDrawerId: String,
    modifier: Modifier = Modifier
) {
    val sortedScores = scores.entries.sortedByDescending { it.value }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // 标题
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "积分榜",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            
            // 积分列表
            sortedScores.forEachIndexed { index, entry ->
                val userName = participantNames[entry.key] ?: "用户"
                val isDrawer = entry.key == currentDrawerId
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(
                            if (isDrawer) PrimaryBlue.copy(alpha = 0.1f) else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 排名
                    val rankIcon = when (index) {
                        0 -> "🥇"
                        1 -> "🥈"
                        2 -> "🥉"
                        else -> "${index + 1}"
                    }
                    Text(
                        text = rankIcon,
                        fontSize = if (index < 3) 20.sp else 14.sp,
                        modifier = Modifier.width(32.dp)
                    )
                    
                    // 头像
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(getUserColor(entry.key)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userName.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // 用户名
                    Text(
                        text = userName + if (isDrawer) " 🎨" else "",
                        modifier = Modifier.weight(1f),
                        fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal
                    )
                    
                    // 积分
                    Text(
                        text = "${entry.value}分",
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                }
            }
        }
    }
}

/**
 * 游戏结束屏幕
 */
@Composable
fun GameEndScreen(
    scores: Map<String, Int>,
    participantNames: Map<String, String>,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sortedScores = scores.entries.sortedByDescending { it.value }
    val winner = sortedScores.firstOrNull()
    val winnerName = winner?.let { participantNames[it.key] } ?: "无"
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 胜利图标
        Text(
            text = "🎉🏆🎉",
            fontSize = 48.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "游戏结束！",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "冠军：$winnerName",
            fontSize = 20.sp,
            color = Color(0xFFFFD700),
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 排名列表
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "最终排名",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                sortedScores.forEachIndexed { index, entry ->
                    val userName = participantNames[entry.key] ?: "用户"
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val rankIcon = when (index) {
                            0 -> "🥇"
                            1 -> "🥈"
                            2 -> "🥉"
                            else -> "${index + 1}."
                        }
                        Text(text = rankIcon, fontSize = 20.sp, modifier = Modifier.width(40.dp))
                        Text(text = userName, modifier = Modifier.weight(1f))
                        Text(
                            text = "${entry.value}分",
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 按钮
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onExit,
                modifier = Modifier.weight(1f)
            ) {
                Text("退出")
            }
            
            Button(
                onClick = onPlayAgain,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("再玩一局")
            }
        }
    }
}

/**
 * 等待开始屏幕
 */
@Composable
fun WaitingScreen(
    participantCount: Int,
    minPlayers: Int = 2,
    isHost: Boolean,
    onStartGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🎮",
            fontSize = 64.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "你画我猜",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "等待其他玩家加入...",
            fontSize = 16.sp,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "$participantCount / $minPlayers 人",
            fontSize = 20.sp,
            color = if (participantCount >= minPlayers) Color(0xFF4CAF50) else Color.Gray
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (isHost && participantCount >= minPlayers) {
            Button(
                onClick = onStartGame,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("开始游戏")
            }
        } else if (!isHost) {
            Text(
                text = "等待房主开始游戏",
                color = Color.Gray
            )
        } else {
            Text(
                text = "至少需要 $minPlayers 人才能开始",
                color = Color.Gray
            )
        }
    }
}

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
