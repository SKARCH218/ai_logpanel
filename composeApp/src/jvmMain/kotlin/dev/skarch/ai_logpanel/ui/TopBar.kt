package dev.skarch.ai_logpanel.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.FrameWindowScope
import org.jetbrains.skia.Image
import java.io.InputStream

@Composable
fun FrameWindowScope.TopBar(
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
    // 로고 이미지 로드
    val logoIcon = try {
        val stream: InputStream? = object {}.javaClass.getResourceAsStream("/logo.png")
        if (stream != null) {
            val bytes = stream.readBytes()
            BitmapPainter(Image.makeFromEncoded(bytes).toComposeImageBitmap())
        } else null
    } catch (e: Exception) {
        null
    }

    Surface(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        color = Color(0xFF13161C),
        shadowElevation = 4.dp
    ) {
        WindowDraggableArea {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 로고 이미지 (클릭하면 메뉴로 이동)
                    if (logoIcon != null) {
                        Image(
                            painter = logoIcon,
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(onClick = onMenu)
                        )
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
}
