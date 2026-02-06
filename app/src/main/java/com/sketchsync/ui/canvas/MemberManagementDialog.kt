package com.sketchsync.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sketchsync.data.model.Room
import com.sketchsync.data.model.RoomRole
import com.sketchsync.ui.theme.PrimaryBlue

/**
 * 成员管理对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberManagementDialog(
    room: Room,
    currentUserId: String,
    onDismiss: () -> Unit,
    onSetRole: (String, RoomRole) -> Unit,
    onKickMember: (String) -> Unit
) {
    val currentUserRole = room.getUserRole(currentUserId)
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column {
                // 标题栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "成员管理 (${room.participants.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }
                
                Divider()
                
                // 成员列表
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(room.participants) { userId ->
                        val userName = room.participantNames[userId] ?: "未知用户"
                        val userRole = room.getUserRole(userId)
                        val isSelf = userId == currentUserId
                        
                        MemberItem(
                            userId = userId,
                            userName = userName,
                            role = userRole,
                            isSelf = isSelf,
                            canManage = currentUserRole == RoomRole.OWNER && !isSelf,
                            onSetRole = { role -> onSetRole(userId, role) },
                            onKick = { onKickMember(userId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MemberItem(
    userId: String,
    userName: String,
    role: RoomRole,
    isSelf: Boolean,
    canManage: Boolean,
    onSetRole: (RoomRole) -> Unit,
    onKick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 头像
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = Color.White
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // 用户名和角色标签
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isSelf) "$userName (我)" else userName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Text(
                text = when(role) {
                    RoomRole.OWNER -> "房主"
                    RoomRole.EDITOR -> "编辑者"
                    RoomRole.VIEWER -> "观看者"
                },
                color = when(role) {
                    RoomRole.OWNER -> Color(0xFFE91E63)     // Pink
                    RoomRole.EDITOR -> Color(0xFF4CAF50)    // Green
                    RoomRole.VIEWER -> Color.Gray
                },
                fontSize = 12.sp
            )
        }
        
        // 管理菜单
        if (canManage) {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "管理")
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("设为编辑者") },
                        onClick = { 
                            onSetRole(RoomRole.EDITOR)
                            showMenu = false
                        }
                    )
                    
                    DropdownMenuItem(
                        text = { Text("设为观看者") },
                        onClick = { 
                            onSetRole(RoomRole.VIEWER)
                            showMenu = false
                        }
                    )
                    
                    Divider()
                    
                    DropdownMenuItem(
                        text = { Text("踢出房间", color = Color.Red) },
                        onClick = { 
                            onKick()
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}
