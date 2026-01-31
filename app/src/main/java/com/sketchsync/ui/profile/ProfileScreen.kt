package com.sketchsync.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sketchsync.data.model.AuthState
import com.sketchsync.data.model.User
import com.sketchsync.ui.auth.AuthViewModel
import com.sketchsync.ui.theme.PrimaryBlue
import com.sketchsync.ui.theme.PrimaryBlueLight

/**
 * 用户资料页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToGallery: () -> Unit,
    onSignOut: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    var showEditDialog by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    
    val user = (authState as? AuthState.Authenticated)?.user
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("个人中心", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // 头部背景和头像
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(PrimaryBlue, PrimaryBlueLight)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // 头像
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user?.displayName?.take(1)?.uppercase() ?: "?",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 用户名
                    Text(
                        text = user?.displayName ?: "未登录",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    // 邮箱
                    Text(
                        text = user?.email ?: "",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 功能菜单
            Column(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                // 编辑资料
                ProfileMenuItem(
                    icon = Icons.Default.Edit,
                    title = "编辑资料",
                    subtitle = "修改用户名",
                    onClick = { showEditDialog = true }
                )
                
                // 我的画廊
                ProfileMenuItem(
                    icon = Icons.Default.Collections,
                    title = "我的画廊",
                    subtitle = "查看保存的作品",
                    onClick = onNavigateToGallery
                )
                
                // 主题设置
                ProfileMenuItem(
                    icon = Icons.Default.Palette,
                    title = "主题设置",
                    subtitle = "深色模式、颜色主题",
                    onClick = { /* TODO */ }
                )
                
                // 设置
                ProfileMenuItem(
                    icon = Icons.Default.Settings,
                    title = "设置",
                    subtitle = "通知、隐私设置",
                    onClick = { /* TODO */ }
                )
                
                // 关于
                ProfileMenuItem(
                    icon = Icons.Default.Info,
                    title = "关于",
                    subtitle = "版本 1.0.0",
                    onClick = { /* TODO */ }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 退出登录按钮
                Button(
                    onClick = { showLogoutConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red.copy(alpha = 0.1f),
                        contentColor = Color.Red
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null)
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text("退出登录")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // 编辑资料对话框
        if (showEditDialog) {
            EditProfileDialog(
                currentName = user?.displayName ?: "",
                isLoading = uiState.isLoading,
                onConfirm = { newName ->
                    viewModel.updateProfile(newName)
                    showEditDialog = false
                },
                onDismiss = { showEditDialog = false }
            )
        }
        
        // 退出确认对话框
        if (showLogoutConfirm) {
            AlertDialog(
                onDismissRequest = { showLogoutConfirm = false },
                title = { Text("退出登录") },
                text = { Text("确定要退出当前账号吗？") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.signOut()
                            showLogoutConfirm = false
                            onSignOut()
                        }
                    ) {
                        Text("退出", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutConfirm = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

/**
 * 菜单项组件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PrimaryBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrimaryBlue
                )
            }
            
            Spacer(modifier = Modifier.padding(8.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

/**
 * 编辑资料对话框
 */
@Composable
fun EditProfileDialog(
    currentName: String,
    isLoading: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑资料") },
        text = {
            Column {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("用户名") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                TextButton(
                    onClick = { onConfirm(newName) },
                    enabled = newName.isNotBlank() && newName != currentName
                ) {
                    Text("保存")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
