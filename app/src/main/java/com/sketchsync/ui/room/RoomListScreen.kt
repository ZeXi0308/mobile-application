package com.sketchsync.ui.room

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material.icons.filled.Person
import com.sketchsync.data.model.GameMode
import com.sketchsync.data.model.Room
import com.sketchsync.ui.theme.PrimaryBlue

/**
 * 房间列表页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomListScreen(
    viewModel: RoomViewModel = hiltViewModel(),
    onNavigateToCanvas: (String) -> Unit,
    onNavigateToProfile: () -> Unit = {},
    onSignOut: () -> Unit
) {
    val rooms by viewModel.rooms.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val roomPresenceCounts by viewModel.roomPresenceCounts.collectAsState()
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinByIdDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    // 监听房间创建/加入成功
    LaunchedEffect(uiState.createdRoomId) {
        uiState.createdRoomId?.let { roomId ->
            viewModel.clearCreatedRoomId()
            onNavigateToCanvas(roomId)
        }
    }
    
    LaunchedEffect(uiState.joinedRoomId) {
        uiState.joinedRoomId?.let { roomId ->
            viewModel.clearJoinedRoomId()
            onNavigateToCanvas(roomId)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SketchSync", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showJoinByIdDialog = true }) {
                        Icon(Icons.Default.Search, contentDescription = "通过ID加入")
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Person, contentDescription = "个人中心")
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "退出登录")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = PrimaryBlue
            ) {
                Icon(Icons.Default.Add, contentDescription = "创建房间", tint = Color.White)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 搜索栏
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.searchRooms(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("搜索房间...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            
            // 房间列表
            if (rooms.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "暂无房间",
                            fontSize = 18.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "点击右下角按钮创建一个吧",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 使用组合key确保presenceCount变化时UI能重新渲染
                    items(
                        items = rooms,
                        key = { room -> "${room.id}_${roomPresenceCounts[room.id] ?: 0}" }
                    ) { room ->
                        RoomCard(
                            room = room,
                            presenceCount = roomPresenceCounts[room.id] ?: 0,
                            onClick = { viewModel.joinRoom(room.id) }
                        )
                    }
                }
            }
        }
        
        // 创建房间对话框
        if (showCreateDialog) {
            CreateRoomDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { name, maxParticipants, isPrivate, password, gameMode ->
                    viewModel.createRoom(name, maxParticipants, isPrivate, password, gameMode)
                    showCreateDialog = false
                }
            )
        }
        
        // 通过ID加入房间对话框
        if (showJoinByIdDialog) {
            JoinByIdDialog(
                onDismiss = { showJoinByIdDialog = false },
                onJoin = { roomId ->
                    viewModel.joinRoomById(roomId)
                    showJoinByIdDialog = false
                }
            )
        }
        
        // 错误提示
        uiState.error?.let { error ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("关闭")
                    }
                }
            ) {
                Text(error)
            }
        }
    }
}

/**
 * 房间卡片
 */
@Composable
fun RoomCard(
    room: Room,
    presenceCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 房间图标
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(
                        when (room.gameMode) {
                            GameMode.FREE_DRAW -> PrimaryBlue
                            GameMode.PICTIONARY -> Color(0xFFFF6B6B)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = room.name.take(2).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 房间信息
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = room.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (room.isPrivate) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "私密",
                            modifier = Modifier.size(16.dp),
                            tint = Color.Gray
                        )
                    }
                }
                
                Text(
                    text = "创建者: ${room.creatorName}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                
                Text(
                    text = when (room.gameMode) {
                        GameMode.FREE_DRAW -> "自由绘画"
                        GameMode.PICTIONARY -> "你画我猜"
                    },
                    fontSize = 12.sp,
                    color = when (room.gameMode) {
                        GameMode.FREE_DRAW -> PrimaryBlue
                        GameMode.PICTIONARY -> Color(0xFFFF6B6B)
                    }
                )
            }
            
            // 人数
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Group,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    // 使用实时presence计数
                    text = "${presenceCount}/${room.maxParticipants}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

/**
 * 创建房间对话框
 */
@Composable
fun CreateRoomDialog(
    onDismiss: () -> Unit,
    onCreate: (String, Int, Boolean, String?, GameMode) -> Unit
) {
    var roomName by remember { mutableStateOf("") }
    var maxParticipants by remember { mutableStateOf("8") }
    var isPrivate by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var gameMode by remember { mutableStateOf(GameMode.FREE_DRAW) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建房间") },
        text = {
            Column {
                OutlinedTextField(
                    value = roomName,
                    onValueChange = { roomName = it },
                    label = { Text("房间名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = maxParticipants,
                    onValueChange = { maxParticipants = it.filter { c -> c.isDigit() } },
                    label = { Text("最大人数") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 游戏模式选择
                Text("游戏模式", fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GameModeChip(
                        text = "自由绘画",
                        selected = gameMode == GameMode.FREE_DRAW,
                        onClick = { gameMode = GameMode.FREE_DRAW }
                    )
                    GameModeChip(
                        text = "你画我猜",
                        selected = gameMode == GameMode.PICTIONARY,
                        onClick = { gameMode = GameMode.PICTIONARY }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCreate(
                        roomName,
                        maxParticipants.toIntOrNull() ?: 8,
                        isPrivate,
                        password.takeIf { it.isNotBlank() },
                        gameMode
                    )
                },
                enabled = roomName.isNotBlank()
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 游戏模式选择芯片
 */
@Composable
fun GameModeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) PrimaryBlue else Color.LightGray.copy(alpha = 0.3f)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color.Black,
            fontSize = 14.sp
        )
    }
}

/**
 * 通过ID加入房间对话框
 */
@Composable
fun JoinByIdDialog(
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit
) {
    var roomId by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("加入房间") },
        text = {
            OutlinedTextField(
                value = roomId,
                onValueChange = { roomId = it },
                label = { Text("房间ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onJoin(roomId) },
                enabled = roomId.isNotBlank()
            ) {
                Text("加入")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
