package dev.skarch.ai_logpanel.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.skarch.ai_logpanel.data.GeminiRepository
import dev.skarch.ai_logpanel.utils.FontLoader
import kotlinx.coroutines.launch

// 에러 로그 사이드바 (오류/경고 색상, 스크롤바)
@Composable
fun ErrorLogSidebar(
    viewModel: MainViewModel,
    serverId: Int,
    errorLogs: List<String>,
    apiKey: String,
    onShowAnalysis: (String, String?) -> Unit = { _, _ -> },
    onRemoveLog: (String) -> Unit = {} // 오류 원문, 분석 결과
) {
    val listState = rememberLazyListState()

    Column(Modifier.fillMaxSize()) {
        Text(
            "에러 로그",
            color = Color(0xFFF44336),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (errorLogs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("에러 없음 ✓", color = Color(0xFF6B7280), fontSize = 16.sp)
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(errorLogs) { log ->
                        ErrorLogCard(viewModel, serverId, log, apiKey, onShowAnalysis, onRemoveLog)
                    }
                }

                // 스크롤바
                VerticalScrollbar(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                while (true) {
                                    awaitPointerEvent()
                                }
                            }
                        },
                    adapter = rememberScrollbarAdapter(listState),
                    style = ScrollbarStyle(
                        minimalHeight = 32.dp,
                        thickness = 6.dp,
                        shape = RoundedCornerShape(3.dp),
                        hoverDurationMillis = 300,
                        unhoverColor = Color(0xFF4B5563),
                        hoverColor = Color(0xFF6B7280)
                    )
                )
            }
        }
    }
}

// 에러 로그 카드 (오류/경고 색상, 버튼 기반 분석)
@Composable
fun ErrorLogCard(
    viewModel: MainViewModel,
    serverId: Int,
    log: String,
    apiKey: String,
    onShowAnalysis: (String, String?) -> Unit = { _, _ -> },
    onRemoveLog: (String) -> Unit = {}
) {
    val logLower = log.lowercase()
    val isError = logLower.contains("error") || logLower.contains("✗") || logLower.contains("[error]")
    val isWarning = logLower.contains("warn") || logLower.contains("warning") || logLower.contains("[warn]")

    // ViewModel에서 저장된 분석 결과 가져오기
    val savedAnalysis = viewModel.getErrorAnalysis(serverId, log)
    var aiAnalysis by remember(savedAnalysis) { mutableStateOf(savedAnalysis) }
    var isAnalyzing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val iconColor = when {
        isError -> Color(0xFFF44336) // 빨강
        isWarning -> Color(0xFFFFC107) // 노랑
        else -> Color(0xFFF44336) // 기본 빨강
    }

    val icon = when {
        isError -> "❌"
        isWarning -> "⚠️"
        else -> "⚠"
    }

    val backgroundColor = when {
        isError -> Color(0xFF252932)
        isWarning -> Color(0xFF2D2520) // 약간 노란 톤
        else -> Color(0xFF252932)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 2.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            // 상단 행: 아이콘 + 로그 + X 버튼
            Row(verticalAlignment = Alignment.Top) {
                Text(icon, fontSize = 18.sp, color = iconColor)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        log,
                        color = if (isWarning) Color(0xFFFFC107) else Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        fontFamily = FontLoader.d2CodingFontFamily
                    )
                }
                // X 버튼 (우측 상단)
                OutlinedButton(
                    onClick = {
                        onRemoveLog(log)
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFEF4444)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                    modifier = Modifier.width(36.dp).height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("✕", fontSize = 16.sp, color = Color(0xFFEF4444))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 버튼 영역 (우측 하단 정렬)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 복사 버튼
                OutlinedButton(
                    onClick = {
                        // 클립보드에 복사
                        try {
                            val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
                            val stringSelection = java.awt.datatransfer.StringSelection(log)
                            clipboard.setContents(stringSelection, null)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF9CA3AF)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF4B5563)),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("📋 복사", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // AI 분석하기 / 결과보기 버튼
                OutlinedButton(
                    onClick = {
                        if (aiAnalysis != null) {
                            // 이미 분석 완료 - 결과 보기
                            onShowAnalysis(log, aiAnalysis)
                        } else {
                            // AI 분석 시작
                            isAnalyzing = true
                            coroutineScope.launch {
                                try {
                                    val geminiRepository = GeminiRepository(apiKey)
                                    geminiRepository.analyzeLog(log).collect { analysis ->
                                        aiAnalysis = analysis
                                        isAnalyzing = false
                                        // 분석 결과를 ViewModel에 저장
                                        viewModel.setErrorAnalysis(serverId, log, analysis)
                                    }
                                } catch (e: Exception) {
                                    aiAnalysis = "분석 실패: ${e.message}"
                                    isAnalyzing = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (aiAnalysis != null) Color(0xFF4CAF50) else Color(0xFF2196F3)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (aiAnalysis != null) Color(0xFF4CAF50) else Color(0xFF2196F3)
                    ),
                    enabled = !isAnalyzing,
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        when {
                            isAnalyzing -> "🔄 분석 중..."
                            aiAnalysis != null -> "📄 결과보기"
                            else -> "🤖 AI 분석하기"
                        },
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

// 콘솔 로그 패널 (자동 스크롤, 오류/경고 색상)
@Composable
fun ConsoleLogPanel(logs: List<String>, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // 사용자가 수동으로 스크롤했는지 확인
    var isUserScrolling by remember { mutableStateOf(false) }

    // 로그가 추가될 때마다 자동 스크롤 (사용자가 스크롤하지 않은 경우)
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty() && !isUserScrolling) {
            // 즉시 스크롤 (애니메이션 없이)
            listState.scrollToItem(logs.size - 1)
        }
    }

    // 스크롤 위치 감지하여 사용자 스크롤 여부 판단
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        if (logs.isNotEmpty()) {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val isAtBottom = lastVisibleIndex >= logs.size - 1

            // 맨 아래에 있으면 자동 스크롤 모드, 아니면 수동 모드
            isUserScrolling = !isAtBottom
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF0D1117),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161B22))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "콘솔 로그",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isUserScrolling) {
                        OutlinedButton(
                            onClick = {
                                isUserScrolling = false
                                coroutineScope.launch {
                                    if (logs.isNotEmpty()) {
                                        listState.scrollToItem(logs.size - 1)
                                    }
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2196F3)),
                            border = BorderStroke(1.dp, Color(0xFF2196F3)),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("↓ 맨 아래로", fontSize = 12.sp)
                        }
                    }
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logs) { log ->
                        LogLine(log)
                    }
                }

                // 스크롤바 (클릭 시 창 이동 방지)
                VerticalScrollbar(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(vertical = 16.dp)
                        .pointerInput(Unit) {
                            // 스크롤바 클릭 이벤트를 소비하여 WindowDraggableArea로 전파 방지
                            awaitEachGesture {
                                while (true) {
                                    awaitPointerEvent()
                                    // 이벤트만 소비하고 아무것도 안함
                                }
                            }
                        },
                    adapter = rememberScrollbarAdapter(listState),
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
        }
    }
}

// 로그 라인 (오류/경고 색상 지원)
@Composable
fun LogLine(log: String) {
    val logLower = log.lowercase()
    val color = when {
        logLower.contains("error") || logLower.contains("✗") || logLower.contains("[error]") -> Color(0xFFF44336) // 빨강
        logLower.contains("warn") || logLower.contains("warning") || logLower.contains("[warn]") -> Color(0xFFFFC107) // 노랑
        logLower.contains("✓") || logLower.contains("success") -> Color(0xFF4CAF50) // 초록
        else -> Color(0xFFE6EDF3) // 기본
    }

    Row {
        Text(
            "›",
            color = Color(0xFF2196F3),
            fontSize = 14.sp,
            modifier = Modifier.width(20.dp),
            fontFamily = FontLoader.d2CodingFontFamily
        )
        Text(
            log,
            color = color,
            fontSize = 13.sp,
            fontFamily = FontLoader.d2CodingFontFamily
        )
    }
}