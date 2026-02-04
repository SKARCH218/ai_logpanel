package dev.skarch.ai_logpanel

import androidx.compose.foundation.background
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.unit.dp

fun main() = application {
    val windowState = rememberWindowState(placement = WindowPlacement.Floating, position = WindowPosition.Aligned(Alignment.Center))
    Window(
        onCloseRequest = ::exitApplication,
        title = "AI-Log Panel (Powered by Gemini)",
        undecorated = true,
        transparent = true,
        state = windowState,
        resizable = true,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp)) // 앱 전체에 굴곡(라운드) 적용
                .background(Color.Transparent)
        ) {
            WindowDraggableArea {
                App(
                    windowState = windowState,
                    onMinimize = { windowState.isMinimized = true },
                    onMaximize = {
                        windowState.placement = if (windowState.placement == WindowPlacement.Maximized) WindowPlacement.Floating else WindowPlacement.Maximized
                    },
                    onClose = ::exitApplication
                )
            }
        }
    }
}
