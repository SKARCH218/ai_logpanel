package dev.skarch.ai_logpanel.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 서버 상태 헤더
@Composable
fun ServerStatusHeader(
    server: Server?,
    isConnected: Boolean,
    isRunning: Boolean,
    onConnect: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onDisconnect: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF252932),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    server?.name ?: "서버 없음",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) Color(0xFF4CAF50) else Color(0xFF6B7280))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isConnected) "연결됨" else "연결 안됨",
                        color = Color(0xFF9CA3AF),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("•", color = Color(0xFF6B7280))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        server?.host ?: "",
                        color = Color(0xFF9CA3AF),
                        fontSize = 14.sp
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Local 서버일 때는 "시작" 버튼만 표시, SSH일 때는 "연결" 후 "시작"
                if (server?.serverType == "Local") {
                    // Local 모드: 시작/중지 버튼만
                    if (!isRunning) {
                        OutlinedButton(
                            onClick = onConnect, // Local에서는 onConnect가 시작 역할
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4CAF50)),
                            border = BorderStroke(1.5.dp, Color(0xFF4CAF50)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("시작", fontWeight = FontWeight.Medium)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onStop,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF44336)),
                            border = BorderStroke(1.5.dp, Color(0xFFF44336)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("중지", fontWeight = FontWeight.Medium)
                        }
                    }
                } else {
                    // SSH 모드: 연결 → 시작 → 중지 흐름
                    if (!isConnected) {
                        OutlinedButton(
                            onClick = onConnect,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2196F3)),
                            border = BorderStroke(1.5.dp, Color(0xFF2196F3)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("연결", fontWeight = FontWeight.Medium)
                        }
                    } else {
                        if (!isRunning) {
                            OutlinedButton(
                                onClick = onStart,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4CAF50)),
                                border = BorderStroke(1.5.dp, Color(0xFF4CAF50)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("시작", fontWeight = FontWeight.Medium)
                            }
                        } else {
                            OutlinedButton(
                                onClick = onStop,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF44336)),
                                border = BorderStroke(1.5.dp, Color(0xFFF44336)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("중지", fontWeight = FontWeight.Medium)
                            }
                        }
                        OutlinedButton(
                            onClick = onDisconnect,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6B7280)),
                            border = BorderStroke(1.dp, Color(0xFF6B7280)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("연결 해제", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}