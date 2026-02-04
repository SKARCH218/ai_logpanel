package dev.skarch.ai_logpanel.utils

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

object FontLoader {
    val d2CodingFontFamily: FontFamily by lazy {
        try {
            val fontFile = java.io.File("composeApp/src/jvmMain/resources/fonts/D2Coding.ttf")
            FontFamily(
                Font(
                    file = fontFile,
                    weight = FontWeight.Normal
                )
            )
        } catch (e: Exception) {
            println("D2Coding 폰트 로드 실패: ${e.message}")
            FontFamily.Monospace // 폴백
        }
    }
}
