package dev.skarch.ai_logpanel.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TopBar(
    onMenu: () -> Unit,
    onMinimize: () -> Unit,
    onMaximize: () -> Unit,
    onClose: () -> Unit,
    onToggleTheme: () -> Unit,
    onShowSettings: () -> Unit,
    onShowAbout: () -> Unit,
    darkMode: Boolean,
    showBackButton: Boolean = true
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        color = Color(0xFF13161C),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showBackButton) {
                    IconButton(onClick = onMenu) {
                        Text("⬅", fontSize = 20.sp, color = Color(0xFF9CA3AF))
                    }
                }
                Text(
                    "AI Log Panel",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WindowControlButton(
                    icon = "−",
                    color = Color(0xFF6B7280),
                    onClick = onMinimize
                )
                WindowControlButton(
                    icon = "◻",
                    color = Color(0xFF6B7280),
                    onClick = onMaximize
                )
                WindowControlButton(
                    icon = "X",
                    color = Color(0xFF6B7280),
                    onClick = onClose
                )
            }
        }
    }
}