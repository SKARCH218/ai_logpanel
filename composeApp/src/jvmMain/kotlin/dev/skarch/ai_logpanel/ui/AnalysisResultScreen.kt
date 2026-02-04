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
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
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
                        modifier = Modifier.weight(1f)
                            .onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                                    if (chatInput.isNotBlank() && !isProcessing) {
                                        val question = chatInput
                                        chatInput = ""

                                        // 질문을 즉시 표시
                                        chatHistory = chatHistory + (question to "AI가 생각중입니다...")
                                        isProcessing = true

                                        coroutineScope.launch {
                                            try {
                                                val geminiRepository = GeminiRepository(apiKey)

                                                // 자유 형식으로 질문 (askFollowUpQuestion 사용)
                                                var answer = ""
                                                geminiRepository.askFollowUpQuestion(
                                                    originalLog = errorLog,
                                                    previousAnalysis = analysis ?: "",
                                                    question = question
                                                ).collect { chunk ->
                                                    answer = chunk
                                                }

                                                // 마지막 질문의 답변 업데이트
                                                chatHistory = chatHistory.dropLast(1) + (question to answer)
                                                isProcessing = false
                                            } catch (e: Exception) {
                                                // 마지막 질문의 답변 업데이트
                                                chatHistory = chatHistory.dropLast(1) + (question to "답변 실패: ${e.message}")
                                                isProcessing = false
                                            }
                                        }
                                    }
                                    true
                                } else {
                                    false
                                }
                            },
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

                                // 질문을 즉시 표시
                                chatHistory = chatHistory + (question to "AI가 생각중입니다...")
                                isProcessing = true

                                coroutineScope.launch {
                                    try {
                                        val geminiRepository = GeminiRepository(apiKey)

                                        // 자유 형식으로 질문 (askFollowUpQuestion 사용)
                                        var answer = ""
                                        geminiRepository.askFollowUpQuestion(
                                            originalLog = errorLog,
                                            previousAnalysis = analysis ?: "",
                                            question = question
                                        ).collect { chunk ->
                                            answer = chunk
                                        }

                                        // 마지막 질문의 답변 업데이트
                                        chatHistory = chatHistory.dropLast(1) + (question to answer)
                                        isProcessing = false
                                    } catch (e: Exception) {
                                        // 마지막 질문의 답변 업데이트
                                        chatHistory = chatHistory.dropLast(1) + (question to "답변 실패: ${e.message}")
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
                        StyledText(
                            line.substring(2),
                            color = Color(0xFFE6EDF3),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier
                        )
                    }
                }
                line.matches(Regex("^\\d+\\.\\s.*")) -> {
                    val parts = line.split(". ", limit = 2)
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text("${parts[0]}. ", color = Color(0xFF2196F3), fontSize = 14.sp)
                        StyledText(
                            parts.getOrNull(1) ?: "",
                            color = Color(0xFFE6EDF3),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier
                        )
                    }
                }
                line.isBlank() -> {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                else -> {
                    // 인라인 스타일 처리 (**, *, `)
                    StyledText(
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

// 볼드, 이탤릭, 인라인 코드를 지원하는 텍스트
@Composable
fun StyledText(
    text: String,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier
) {
    val annotatedString = buildAnnotatedString {
        var currentIndex = 0
        val textLength = text.length

        while (currentIndex < textLength) {
            when {
                // 볼드 처리 (**텍스트**)
                text.startsWith("**", currentIndex) -> {
                    val endIndex = text.indexOf("**", currentIndex + 2)
                    if (endIndex != -1) {
                        val boldText = text.substring(currentIndex + 2, endIndex)
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) {
                            append(boldText)
                        }
                        currentIndex = endIndex + 2
                    } else {
                        append(text[currentIndex])
                        currentIndex++
                    }
                }
                // 인라인 코드 처리 (`텍스트`)
                text.startsWith("`", currentIndex) && !text.startsWith("```", currentIndex) -> {
                    val endIndex = text.indexOf("`", currentIndex + 1)
                    if (endIndex != -1) {
                        val codeText = text.substring(currentIndex + 1, endIndex)
                        withStyle(SpanStyle(
                            color = Color(0xFF58A6FF),
                            fontFamily = FontLoader.d2CodingFontFamily,
                            background = Color(0xFF0D1117)
                        )) {
                            append(codeText)
                        }
                        currentIndex = endIndex + 1
                    } else {
                        append(text[currentIndex])
                        currentIndex++
                    }
                }
                // 이탤릭 처리 (*텍스트*)
                text.startsWith("*", currentIndex) && !text.startsWith("**", currentIndex) -> {
                    val endIndex = text.indexOf("*", currentIndex + 1)
                    if (endIndex != -1 && !text.startsWith("*", endIndex + 1)) {
                        val italicText = text.substring(currentIndex + 1, endIndex)
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = Color(0xFFB4BEFE))) {
                            append(italicText)
                        }
                        currentIndex = endIndex + 1
                    } else {
                        append(text[currentIndex])
                        currentIndex++
                    }
                }
                else -> {
                    append(text[currentIndex])
                    currentIndex++
                }
            }
        }
    }

    Text(
        text = annotatedString,
        fontSize = fontSize,
        lineHeight = lineHeight,
        modifier = modifier
    )
}

