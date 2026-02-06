package com.sketchsync.ui.canvas

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Square
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.awaitPointerEvent
import androidx.compose.ui.input.pointer.awaitPointerEventScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.sketchsync.data.model.DrawTool
import com.sketchsync.data.model.RoomRole
import com.sketchsync.util.ReplayState
import com.sketchsync.ui.theme.DrawingColors
import com.sketchsync.ui.theme.PrimaryBlue

/**
 * 画布界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasScreen(
    roomId: String,
    viewModel: CanvasViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val room by viewModel.room.collectAsState()
    val remotePaths by viewModel.remotePaths.collectAsState()
    val allPaths by viewModel.allPaths.collectAsState()
    val cursors by viewModel.cursors.collectAsState()
    val clearEvent by viewModel.clearEvent.collectAsState()
    val context = LocalContext.current
    
    var drawingCanvas by remember { mutableStateOf<DrawingCanvas?>(null) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showStrokeSlider by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showTextInput by remember { mutableStateOf(false) }
    var textPosition by remember { mutableStateOf(Pair(0f, 0f)) }
    var showMemberDialog by remember { mutableStateOf(false) }
    
    // 获取当前用户角色
    val currentUserRole = remember(room, viewModel.currentUserId) {
        room?.getUserRole(viewModel.currentUserId ?: "") ?: RoomRole.EDITOR
    }
    val isViewer = currentUserRole == RoomRole.VIEWER
    
    // 回放状态
    val replayState by viewModel.replayState.collectAsState()
    val replayProgress by viewModel.replayProgress.collectAsState()
    val replayPathIds by viewModel.replayPathIds.collectAsState()
    val isReplaying = replayState != ReplayState.IDLE
    
    // 麦克风权限
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.joinVoiceChannel()
        } else {
            Toast.makeText(context, "需要麦克风权限才能语音聊天", Toast.LENGTH_SHORT).show()
        }
    }
    
    // 初始化
    LaunchedEffect(roomId) {
        viewModel.joinRoom(roomId)
    }
    
    // 追踪已渲染的路径ID
    val renderedPathIds = remember { mutableSetOf<String>() }
    
    // 监听远程路径变化
    // 监听远程路径变化和回放状态
    LaunchedEffect(allPaths, remotePaths, drawingCanvas, replayState, replayPathIds) {
        if (drawingCanvas == null) return@LaunchedEffect
        
        if (replayState != ReplayState.IDLE) {
            // 回放模式：只渲染回放路径，不影响原始画布状态
            val pathsToShow = allPaths.filter { it.id in replayPathIds }
            drawingCanvas?.setReplayPaths(pathsToShow)
        } else {
            // 普通模式：增量添加
            remotePaths.forEach { path ->
                // 只添加尚未渲染的路径
                if (path.id !in renderedPathIds) {
                    drawingCanvas?.addRemotePath(path)
                    renderedPathIds.add(path.id)
                }
            }
        }
    }
    
    // 监听在线用户
    val onlineUsers by viewModel.onlineUsers.collectAsState()
    
    // 监听清空事件
    var lastClearTime by remember { mutableStateOf(0L) }
    LaunchedEffect(clearEvent) {
        if (clearEvent > lastClearTime && clearEvent > 0) {
            drawingCanvas?.clear()
            renderedPathIds.clear()
            lastClearTime = clearEvent
        }
    }
    
    // 监听光标变化
    LaunchedEffect(cursors) {
        cursors.forEach { (userId, position) ->
            drawingCanvas?.updateCursor(userId, position.first, position.second)
        }
    }
    
    // 清理
    DisposableEffect(Unit) {
        onDispose {
            viewModel.leaveRoom()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = room?.name ?: "画布",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            // 使用实时在线人数
                            text = "${onlineUsers.size}人在线",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 语音按钮
                    IconButton(
                        onClick = {
                            android.util.Log.d("CanvasScreen", "Voice button clicked! isVoiceEnabled=${uiState.isVoiceEnabled}, isVoiceConnecting=${uiState.isVoiceConnecting}")
                            if (uiState.isVoiceEnabled) {
                                android.util.Log.d("CanvasScreen", "Toggling mute")
                                viewModel.toggleMute()
                            } else if (!uiState.isVoiceConnecting) {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                android.util.Log.d("CanvasScreen", "Has mic permission: $hasPermission")
                                
                                if (hasPermission) {
                                    android.util.Log.d("CanvasScreen", "Calling joinVoiceChannel()")
                                    viewModel.joinVoiceChannel()
                                } else {
                                    android.util.Log.d("CanvasScreen", "Requesting mic permission")
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            } else {
                                android.util.Log.d("CanvasScreen", "Already connecting, ignoring click")
                            }
                        }
                    ) {
                        if (uiState.isVoiceConnecting) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = PrimaryBlue,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (uiState.isMuted || !uiState.isVoiceEnabled) 
                                    Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "语音",
                                tint = if (uiState.isVoiceEnabled && !uiState.isMuted) 
                                    Color(0xFF4CAF50) else Color.Gray
                            )
                        }
                    }
                    
                    // 成员管理按钮 (仅房主)
                    if (currentUserRole == RoomRole.OWNER) {
                        IconButton(onClick = { showMemberDialog = true }) {
                            Icon(Icons.Default.Person, contentDescription = "成员管理")
                        }
                    }
                    
                    // 保存按钮
                    IconButton(
                        onClick = {
                            drawingCanvas?.exportToBytes()?.let { bytes ->
                                viewModel.exportCanvas(bytes)
                            }
                        }
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "保存")
                    }
                    
                    // 回放按钮
                    IconButton(
                        onClick = {
                            if (isReplaying) {
                                viewModel.stopReplay()
                            } else {
                                viewModel.startReplay()
                            }
                        }
                    ) {
                        Icon(
                            if (isReplaying) Icons.Default.Cancel else Icons.Default.PlayArrow,
                            contentDescription = if (isReplaying) "停止回放" else "回放"
                        )
                    }
                    
                    // 清空按钮 (Viewer不可用)
                    if (!isViewer) {
                        IconButton(onClick = { showClearConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "清空", tint = Color.Red)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 绘画区域 (包含回放控制条)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        DrawingCanvas(ctx).apply {
                            drawingCanvas = this
                            setTool(uiState.currentTool)
                            setColor(uiState.currentColor)
                            setStrokeWidth(uiState.currentStrokeWidth)
                            setReadOnly(isViewer || isReplaying)
                            setReplayMode(isReplaying)
                            
                            onPathCompleted = { path ->
                                viewModel.sendPath(path)
                            }
                            
                            onCursorMoved = { x, y ->
                                viewModel.updateCursor(x, y)
                            }
                            
                            onTextToolClicked = { x, y ->
                                textPosition = Pair(x, y)
                                showTextInput = true
                            }
                        }
                    },
                    update = { canvas ->
                        canvas.setTool(uiState.currentTool)
                        canvas.setColor(uiState.currentColor)
                        canvas.setStrokeWidth(uiState.currentStrokeWidth)
                        canvas.setReadOnly(isViewer || isReplaying)
                        canvas.setReplayMode(isReplaying)
                    }
                )
                
                if (isViewer || isReplaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        awaitPointerEvent()
                                    }
                                }
                            }
                    )
                }
                
                // 回放控制条
                if (isReplaying) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .fillMaxWidth(0.9f),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Slider(
                                value = replayProgress,
                                onValueChange = { viewModel.seekReplay(it) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                IconButton(
                                    onClick = { 
                                        if (replayState == ReplayState.PLAYING) viewModel.pauseReplay() 
                                        else viewModel.resumeReplay() 
                                    }
                                ) {
                                    Icon(
                                        if (replayState == ReplayState.PLAYING) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (replayState == ReplayState.PLAYING) "暂停" else "播放"
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // 工具栏 (Viewer模式和回放模式下隐藏)
            if (!isViewer && !isReplaying) {
                ToolBar(
                    currentTool = uiState.currentTool,
                    currentColor = uiState.currentColor,
                    currentStrokeWidth = uiState.currentStrokeWidth,
                    canUndo = drawingCanvas?.canUndo() ?: false,
                    canRedo = drawingCanvas?.canRedo() ?: false,
                    onToolSelected = { tool ->
                        viewModel.setTool(tool)
                        drawingCanvas?.setTool(tool)
                    },
                    onColorClick = { showColorPicker = true },
                    onStrokeClick = { showStrokeSlider = true },
                    onUndo = { drawingCanvas?.undo() },
                    onRedo = { drawingCanvas?.redo() }
                )
            }
        }
        
        // 颜色选择器
        if (showColorPicker) {
            ColorPickerDialog(
                currentColor = uiState.currentColor,
                onColorSelected = { color ->
                    viewModel.setColor(color)
                    drawingCanvas?.setColor(color)
                    showColorPicker = false
                },
                onDismiss = { showColorPicker = false }
            )
        }
        
        // 画笔粗细滑块
        if (showStrokeSlider) {
            StrokeSliderDialog(
                currentWidth = uiState.currentStrokeWidth,
                onWidthChanged = { width ->
                    viewModel.setStrokeWidth(width)
                    drawingCanvas?.setStrokeWidth(width)
                },
                onDismiss = { showStrokeSlider = false }
            )
        }
        
        // 清空确认
        if (showClearConfirm) {
            AlertDialog(
                onDismissRequest = { showClearConfirm = false },
                title = { Text("清空画布") },
                text = { Text("确定要清空所有绘图内容吗？此操作不可撤销。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            drawingCanvas?.clear()
                            viewModel.clearCanvas()
                            showClearConfirm = false
                        }
                    ) {
                        Text("清空", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirm = false }) {
                        Text("取消")
                    }
                }
            )
        }
        
        // 消息提示
        uiState.message?.let { message ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                containerColor = Color(0xFF4CAF50),
                action = {
                    TextButton(onClick = { viewModel.clearMessage() }) {
                        Text("关闭", color = Color.White)
                    }
                }
            ) {
                Text(message, color = Color.White)
            }
        }
        
        uiState.error?.let { error ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearMessage() }) {
                        Text("关闭")
                    }
                }
            ) {
                Text(error)
            }
        }
        
        // 文字输入对话框
        if (showTextInput) {
            TextInputDialog(
                onDismiss = { showTextInput = false },
                onConfirm = { text, fontSize ->
                    // 在画布中心位置添加文字（可以根据点击位置调整）
                    val x = textPosition.first
                    val y = textPosition.second
                    
                    drawingCanvas?.addTextPath(x, y, text, fontSize)?.let { path ->
                        viewModel.sendPath(path)
                    }
                    showTextInput = false
                },
                currentColor = uiState.currentColor
            )
        }
        
        // 成员管理对话框
        if (showMemberDialog && currentUserRole == RoomRole.OWNER) {
            room?.let { r ->
                MemberManagementDialog(
                    room = r,
                    currentUserId = viewModel.currentUserId ?: "",
                    onDismiss = { showMemberDialog = false },
                    onSetRole = { userId, role ->
                        viewModel.setMemberRole(userId, role.name)
                    },
                    onKickMember = { userId ->
                        viewModel.kickMember(userId)
                    }
                )
            }
        }
    }
}

/**
 * 工具栏
 */
@Composable
fun ToolBar(
    currentTool: DrawTool,
    currentColor: Int,
    currentStrokeWidth: Float,
    canUndo: Boolean,
    canRedo: Boolean,
    onToolSelected: (DrawTool) -> Unit,
    onColorClick: () -> Unit,
    onStrokeClick: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 画笔
            ToolButton(
                icon = Icons.Default.Brush,
                selected = currentTool == DrawTool.BRUSH,
                onClick = { onToolSelected(DrawTool.BRUSH) },
                contentDescription = "画笔"
            )
            
            // 橡皮擦
            ToolButton(
                icon = Icons.Default.Clear,
                selected = currentTool == DrawTool.ERASER,
                onClick = { onToolSelected(DrawTool.ERASER) },
                contentDescription = "橡皮擦"
            )
            
            // 直线
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (currentTool == DrawTool.LINE) PrimaryBlue.copy(alpha = 0.2f)
                        else Color.Transparent
                    )
                    .clickable { onToolSelected(DrawTool.LINE) },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(2.dp)
                        .background(
                            if (currentTool == DrawTool.LINE) PrimaryBlue else Color.Gray
                        )
                )
            }
            
            // 矩形
            ToolButton(
                icon = Icons.Default.Square,
                selected = currentTool == DrawTool.RECTANGLE,
                onClick = { onToolSelected(DrawTool.RECTANGLE) },
                contentDescription = "矩形"
            )
            
            // 圆形
            ToolButton(
                icon = Icons.Default.RadioButtonUnchecked,
                selected = currentTool == DrawTool.CIRCLE,
                onClick = { onToolSelected(DrawTool.CIRCLE) },
                contentDescription = "圆形"
            )
            
            // 文字工具
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (currentTool == DrawTool.TEXT) PrimaryBlue.copy(alpha = 0.2f)
                        else Color.Transparent
                    )
                    .clickable { onToolSelected(DrawTool.TEXT) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "T",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (currentTool == DrawTool.TEXT) PrimaryBlue else Color.Gray
                )
            }
            
            // 拖拽/平移 (使用自定义图标)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (currentTool == DrawTool.PAN) PrimaryBlue.copy(alpha = 0.2f)
                        else Color.Transparent
                    )
                    .clickable { onToolSelected(DrawTool.PAN) },
                contentAlignment = Alignment.Center
            ) {
                // 绘制十字箭头表示移动
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // 上箭头
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(8.dp)
                            .background(if (currentTool == DrawTool.PAN) PrimaryBlue else Color.Gray)
                    )
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 左箭头
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .height(2.dp)
                                .background(if (currentTool == DrawTool.PAN) PrimaryBlue else Color.Gray)
                        )
                        // 中心点
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(if (currentTool == DrawTool.PAN) PrimaryBlue else Color.Gray)
                        )
                        // 右箭头
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .height(2.dp)
                                .background(if (currentTool == DrawTool.PAN) PrimaryBlue else Color.Gray)
                        )
                    }
                    // 下箭头
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(8.dp)
                            .background(if (currentTool == DrawTool.PAN) PrimaryBlue else Color.Gray)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 颜色选择
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(currentColor))
                    .border(2.dp, Color.Gray, CircleShape)
                    .clickable { onColorClick() }
            )
            
            // 粗细选择
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray.copy(alpha = 0.3f))
                    .clickable { onStrokeClick() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size((currentStrokeWidth.coerceIn(4f, 20f)).dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 撤销
            IconButton(
                onClick = onUndo,
                enabled = canUndo
            ) {
                Icon(
                    Icons.Default.Undo,
                    contentDescription = "撤销",
                    tint = if (canUndo) PrimaryBlue else Color.Gray
                )
            }
            
            // 重做
            IconButton(
                onClick = onRedo,
                enabled = canRedo
            ) {
                Icon(
                    Icons.Default.Redo,
                    contentDescription = "重做",
                    tint = if (canRedo) PrimaryBlue else Color.Gray
                )
            }
        }
    }
}

/**
 * 工具按钮
 */
@Composable
fun ToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    contentDescription: String
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                if (selected) PrimaryBlue.copy(alpha = 0.2f) else Color.Transparent
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (selected) PrimaryBlue else Color.Gray
        )
    }
}

/**
 * 颜色选择器对话框
 */
@Composable
fun ColorPickerDialog(
    currentColor: Int,
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择颜色") },
        text = {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(DrawingColors) { color ->
                    val colorInt = color.toArgb()
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (colorInt == currentColor) 3.dp else 1.dp,
                                color = if (colorInt == currentColor) PrimaryBlue else Color.Gray,
                                shape = CircleShape
                            )
                            .clickable { onColorSelected(colorInt) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

/**
 * 画笔粗细滑块对话框
 */
@Composable
fun StrokeSliderDialog(
    currentWidth: Float,
    onWidthChanged: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(currentWidth) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("画笔粗细") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("${sliderValue.toInt()} px")
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = sliderValue,
                    onValueChange = {
                        sliderValue = it
                        onWidthChanged(it)
                    },
                    valueRange = 2f..50f,
                    steps = 47
                )
                Spacer(modifier = Modifier.height(8.dp))
                // 预览
                Box(
                    modifier = Modifier
                        .size(sliderValue.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        }
    )
}
