package com.sketchsync.ui.canvas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * 文字输入对话框
 * 用于在画布上添加文字
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextInputDialog(
    onDismiss: () -> Unit,
    onConfirm: (text: String, fontSize: Float) -> Unit,
    currentColor: Int
) {
    var text by remember { mutableStateOf("") }
    var selectedSize by remember { mutableStateOf(FontSize.MEDIUM) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "添加文字",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 文字输入框
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("输入文字") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(currentColor),
                        focusedLabelColor = Color(currentColor)
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 字号选择
                Text(
                    text = "字号",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Start)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FontSize.entries.forEach { size ->
                        FilterChip(
                            selected = selectedSize == size,
                            onClick = { selectedSize = size },
                            label = { 
                                Text(
                                    text = size.label,
                                    fontSize = size.previewSize.sp
                                ) 
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(currentColor).copy(alpha = 0.2f),
                                selectedLabelColor = Color(currentColor)
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("取消")
                    }
                    
                    Button(
                        onClick = {
                            if (text.isNotBlank()) {
                                onConfirm(text, selectedSize.fontSize)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = text.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(currentColor)
                        )
                    ) {
                        Text("确定", color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * 字号枚举
 */
enum class FontSize(val label: String, val fontSize: Float, val previewSize: Int) {
    SMALL("小", 32f, 12),
    MEDIUM("中", 48f, 16),
    LARGE("大", 72f, 20),
    XLARGE("特大", 96f, 24)
}
