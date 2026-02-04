package dev.skarch.ai_logpanel.ui

import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.skarch.ai_logpanel.data.GeminiRepository
import dev.skarch.ai_logpanel.utils.FontLoader
import kotlinx.coroutines.launch

// AI 분석 결과 화면
@Composable
fun AnalysisResultScreen(
    errorLog: String,
    analysis: String?,
    apiKey: String,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    var currentAnalysis by remember { mutableStateOf(analysis) }
    var chatInput by remember { mutableStateOf("") }
    var chatHistory by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) } // (질문, 답변)
    var isProcessing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1D23))) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 상단바 (뒤로가기)
            Surface(
                modifier = Modifier.fillMaxWidth().height(60.dp),
                color = Color(0xFF1E1F22),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF2196F3)
                        ),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("← 뒤로가기", fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "AI 오류 분석 결과",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 스크롤 가능한 콘텐츠 영역
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(32.dp)
                ) {
                    // 오류 원문 섹션
                    Text(
                        "📋 오류 원문",
                        color = Color(0xFFF44336),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF0D1117),
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 2.dp
                    ) {
                        Text(
                            errorLog,
                            color = Color(0xFFF44336),
                            fontSize = 14.sp,
                            fontFamily = FontLoader.d2CodingFontFamily,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // 분석 결과 섹션
                    Text(
                        "🤖 AI 분석 결과",
                        color = Color(0xFF2196F3),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF252932),
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 2.dp
                    ) {
                        if (currentAnalysis != null) {
                            MarkdownText(
                                markdown = currentAnalysis!!,
                                modifier = Modifier.padding(20.dp)
                            )
                        } else {
                            Text(
                                "분석 중...",
                                color = Color(0xFF9CA3AF),
                                fontSize = 14.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    // 채팅 히스토리
                    if (chatHistory.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            "💬 추가 질문 & 답변",
                            color = Color(0xFF4CAF50),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        chatHistory.forEach { (question, answer) ->
                            // 질문
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                color = Color(0xFF1C1F26),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "❓ 질문",
                                        color = Color(0xFFFFC107),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        question,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            // 답변
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                color = Color(0xFF252932),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "💡 답변",
                                        color = Color(0xFF4CAF50),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    MarkdownText(
                                        markdown = answer,
                                        modifier = Modifier
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(80.dp)) // 입력창 공간 확보
                }

                // 스크롤바
                VerticalScrollbar(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(vertical = 8.dp),
                    adapter = rememberScrollbarAdapter(scrollState),
                    style = ScrollbarStyle(
                        minimalHeight = 32.dp,
                        thickness = 8.dp,
                        shape = RoundedCornerShape(4.dp),
                        hoverDurationMillis = 300,
                        unhoverColor = Color(0xFF4B5563),
                        hoverColor = Color(0xFF6B7280)
                    )
                )
            }

            // 하단 질문 입력창
            HorizontalDivider(color = Color(0xFF313338), thickness = 1.dp)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF1E1F22),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = chatInput,
                        onValueChange = { chatInput = it },
                        placeholder = {
                            Text(
                                "이 오류에 대해 추가로 질문하기...",
                                color = Color(0xFF6B7280),
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF2196F3),
                            unfocusedBorderColor = Color(0xFF4B5563),
                            cursorColor = Color(0xFF2196F3)
                        ),
                        enabled = !isProcessing,
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (chatInput.isNotBlank() && !isProcessing) {
                                val question = chatInput
                                chatInput = ""
                                isProcessing = true

                                coroutineScope.launch {
                                    try {
                                        val geminiRepository = GeminiRepository(apiKey)
                                        val context = """
                                            원본 오류:
                                            $errorLog
                                            
                                            이전 분석 결과:
                                            ${currentAnalysis ?: "없음"}
                                            
                                            사용자 질문: $question
                                        """.trimIndent()

                                        var answer = ""
                                        geminiRepository.analyzeLog(context).collect { chunk ->
                                            answer = chunk
                                        }

                                        chatHistory = chatHistory + (question to answer)
                                        isProcessing = false
                                    } catch (e: Exception) {
                                        chatHistory = chatHistory + (question to "답변 실패: ${e.message}")
                                        isProcessing = false
                                    }
                                }
                            }
                        },
                        enabled = chatInput.isNotBlank() && !isProcessing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3),
                            disabledContainerColor = Color(0xFF4B5563)
                        ),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text(
                            if (isProcessing) "전송 중..." else "전송",
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

// 간단한 Markdown 렌더러 (기본적인 스타일만 지원)
@Composable
fun MarkdownText(markdown: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        val lines = markdown.split("\n")
        var inCodeBlock = false
        val codeBlockLines = mutableListOf<String>()

        lines.forEach { line ->
            when {
                line.startsWith("```") -> {
                    if (inCodeBlock) {
                        // 코드 블록 종료
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            color = Color(0xFF0D1117),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                codeBlockLines.joinToString("\n"),
                                color = Color(0xFF58A6FF),
                                fontSize = 13.sp,
                                fontFamily = FontLoader.d2CodingFontFamily,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        codeBlockLines.clear()
                        inCodeBlock = false
                    } else {
                        // 코드 블록 시작
                        inCodeBlock = true
                    }
                }
                inCodeBlock -> {
                    codeBlockLines.add(line)
                }
                line.startsWith("# ") -> {
                    Text(
                        line.removePrefix("# "),
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                line.startsWith("## ") -> {
                    Text(
                        line.removePrefix("## "),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
                line.startsWith("### ") -> {
                    Text(
                        line.removePrefix("### "),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text("• ", color = Color(0xFF2196F3), fontSize = 14.sp)
                        Text(
                            line.substring(2),
                            color = Color(0xFFE6EDF3),
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
                line.matches(Regex("^\\d+\\.\\s.*")) -> {
                    val parts = line.split(". ", limit = 2)
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text("${parts[0]}. ", color = Color(0xFF2196F3), fontSize = 14.sp)
                        Text(
                            parts.getOrNull(1) ?: "",
                            color = Color(0xFFE6EDF3),
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
                line.isBlank() -> {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                else -> {
                    Text(
                        line,
                        color = Color(0xFFE6EDF3),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}
