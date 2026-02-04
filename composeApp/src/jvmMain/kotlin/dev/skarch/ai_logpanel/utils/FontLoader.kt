package dev.skarch.ai_logpanel.utils

import androidx.compose.ui.text.font.FontFamily

object FontLoader {
    val d2CodingFontFamily: FontFamily by lazy {
        try {
            // Compose Multiplatform JVM에서는 기본 Monospace 폰트 사용
            // 커스텀 폰트 로딩은 플랫폼별 구현이 필요함
            FontFamily.Monospace
        } catch (e: Exception) {
            FontFamily.Monospace
        }
    }
}
