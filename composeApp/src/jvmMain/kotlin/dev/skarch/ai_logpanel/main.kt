package dev.skarch.ai_logpanel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import org.jetbrains.skia.Image
import java.io.InputStream

fun main() = application {
    val windowState = rememberWindowState(
        placement = WindowPlacement.Floating,
        position = WindowPosition.Aligned(Alignment.Center),
        size = DpSize(1200.dp, 800.dp) // 초기 크기
    )

    // 로고 이미지 로드
    val logoIcon = try {
        val stream: InputStream? = object {}.javaClass.getResourceAsStream("/logo.png")
        if (stream != null) {
            val bytes = stream.readBytes()
            BitmapPainter(Image.makeFromEncoded(bytes).toComposeImageBitmap())
        } else null
    } catch (e: Exception) {
        println("로고 로드 실패: ${e.message}")
        null
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "AI-Log Panel (Powered by Gemini)",
        icon = logoIcon,
        undecorated = true,
        transparent = true,
        state = windowState,
        resizable = true,
    ) {
        // 최소 창 크기 제한
        LaunchedEffect(windowState.size) {
            val minWidth = 1000.dp
            val minHeight = 700.dp

            if (windowState.size.width < minWidth || windowState.size.height < minHeight) {
                windowState.size = DpSize(
                    width = maxOf(windowState.size.width, minWidth),
                    height = maxOf(windowState.size.height, minHeight)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp)) // 앱 전체에 굴곡(라운드) 적용
                .background(Color.Transparent)
        ) {
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
