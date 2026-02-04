package dev.skarch.ai_logpanel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.skarch.ai_logpanel.ssh.SshClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

// 전문 서버 관리 대시보드 스타일 패널
@Composable
fun MainPanelImproved(
    viewModel: MainViewModel,
    server: Server?,
    sshClient: SshClient?,
    onConnect: suspend () -> Unit,
    onExec: suspend (String) -> String,
    onDisconnect: () -> Unit,
    apiKey: String
) {
    var commandInput by remember { mutableStateOf("") }
    var sidebarTab by remember { mutableStateOf<Int?>(null) } // null: 닫힘, 0: 에러, 1: 성능
    val pointColor = Color(0xFF2196F3)

    // 분석 결과 화면 상태
    var showAnalysisResult by remember { mutableStateOf(false) }
    var selectedErrorLog by remember { mutableStateOf<String?>(null) }
    var selectedAnalysis by remember { mutableStateOf<String?>(null) }

    // ViewModel에서 서버 상태 가져오기 (화면 전환 시에도 유지됨)
    val currentServer = server?.id?.let { viewModel.getServer(it) } ?: server
    val logs = currentServer?.logs ?: emptyList()
    val isRunning = currentServer?.isRunning ?: false
    val isConnectedFromServer = currentServer?.isConnected ?: false

    val errorLogs = remember(logs) {
        logs.filter { it.contains("error", true) || it.contains("fail", true) || it.contains("[ERROR]", true) }
    }

    // 성능 데이터
    var cpuUsage by remember { mutableStateOf(0f) }
    var ramUsage by remember { mutableStateOf(0f) }
    var netUsage by remember { mutableStateOf(0f) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // 로컬 연결 상태 (ViewModel과 동기화)
    var isConnected by remember(isConnectedFromServer) { mutableStateOf(isConnectedFromServer) }
    var connectionError by remember { mutableStateOf<String?>(null) }

    // 로컬 서버 프로세스 및 입출력 스트림 저장
    var localServerProcess by remember { mutableStateOf<Process?>(null) }
    var processOutputStream by remember { mutableStateOf<OutputStream?>(null) }

    // 헬퍼 함수: 로그 추가 (ViewModel에 저장)
    fun addLog(message: String) {
        server?.id?.let { viewModel.addLog(it, message) }
    }

    // 헬퍼 함수: 서버 실행 상태 설정 (ViewModel에 저장)
    fun setRunning(running: Boolean) {
        server?.id?.let { viewModel.setServerRunning(it, running) }
    }

    // 헬퍼 함수: 연결 상태 설정 (ViewModel에 저장)
    fun setConnected(connected: Boolean) {
        isConnected = connected
        server?.id?.let { viewModel.setServerConnected(it, connected) }
    }

    // SSH 클라이언트 에러 핸들러 설정
    LaunchedEffect(sshClient) {
        sshClient?.onError = { error ->
            connectionError = error
            coroutineScope.launch {
                snackbarHostState.showSnackbar(error)
            }
        }
    }

    // 연결되면 주기적으로 성능 데이터 가져오기 (5초마다)
    LaunchedEffect(isConnected, server?.serverType) {
        if (isConnected) {
            while (isConnected) {
                if (server?.serverType == "Local") {
                    // Local 모드: 로컬 시스템 성능 데이터 가져오기
                    withContext(Dispatchers.IO) {
                        try {
                            // CPU 사용률
                            val cpuResult = if (System.getProperty("os.name").startsWith("Windows")) {
                                val process = ProcessBuilder("cmd.exe", "/c", "wmic cpu get loadpercentage /value")
                                    .start()
                                val output = process.inputStream.bufferedReader(Charsets.UTF_8).readText()
                                process.waitFor()

                                val regex = "LoadPercentage=(\\d+)".toRegex()
                                regex.find(output)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
                            } else {
                                val process = ProcessBuilder("bash", "-c", "top -bn1 | grep 'Cpu(s)' | sed 's/.*, *\\([0-9.]*\\)%* id.*/\\1/' | awk '{print 100 - \$1}'")
                                    .start()
                                val output = process.inputStream.bufferedReader().readText().trim()
                                process.waitFor()
                                output.toFloatOrNull() ?: 0f
                            }
                            cpuUsage = cpuResult

                            // RAM 사용률
                            val ramResult = if (System.getProperty("os.name").startsWith("Windows")) {
                                val process = ProcessBuilder("cmd.exe", "/c", "wmic OS get FreePhysicalMemory,TotalVisibleMemorySize /value")
                                    .start()
                                val output = process.inputStream.bufferedReader(Charsets.UTF_8).readText()
                                process.waitFor()

                                val freeRegex = "FreePhysicalMemory=(\\d+)".toRegex()
                                val totalRegex = "TotalVisibleMemorySize=(\\d+)".toRegex()
                                val free = freeRegex.find(output)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
                                val total = totalRegex.find(output)?.groupValues?.get(1)?.toFloatOrNull() ?: 1f

                                ((total - free) / total) * 100f
                            } else {
                                val process = ProcessBuilder("bash", "-c", "free | grep Mem | awk '{print (\$3/\$2) * 100.0}'")
                                    .start()
                                val output = process.inputStream.bufferedReader().readText().trim()
                                process.waitFor()
                                output.toFloatOrNull() ?: 0f
                            }
                            ramUsage = ramResult

                            // 네트워크 사용량
                            val netResult = if (System.getProperty("os.name").startsWith("Windows")) {
                                0f // Windows는 간단한 측정 어려움
                            } else {
                                val process = ProcessBuilder("bash", "-c", "cat /proc/net/dev | grep -E 'eth0|ens|wlan' | head -1 | awk '{print (\$2+\$10)/1024/1024}'")
                                    .start()
                                val output = process.inputStream.bufferedReader().readText().trim()
                                process.waitFor()
                                output.toFloatOrNull() ?: 0f
                            }
                            netUsage = netResult
                        } catch (e: Exception) {
                            // 성능 데이터 가져오기 실패 시 무시
                        }
                    }
                } else {
                    // SSH 모드: SSH로 성능 데이터 가져오기
                    sshClient?.getPerformanceData()?.let { result ->
                        if (result.isSuccess) {
                            val data = result.getOrNull()
                            data?.let {
                                cpuUsage = it.cpu
                                ramUsage = it.ram
                                netUsage = it.network
                            }
                        }
                    }
                }
                delay(5000) // 5초 대기
            }
        }
    }

    // 컴포넌트가 제거될 때 프로세스 정리
    DisposableEffect(Unit) {
        onDispose {
            // OutputStream 닫기
            try {
                processOutputStream?.close()
            } catch (e: Exception) {
                // 무시
            }

            // Local 서버 프로세스 종료
            localServerProcess?.let { process ->
                if (process.isAlive) {
                    process.destroy()
                    // 강제 종료
                    if (!process.waitFor(2, TimeUnit.SECONDS)) {
                        process.destroyForcibly()
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1D23))) {
        // 분석 결과 화면 표시
        if (showAnalysisResult && selectedErrorLog != null) {
            AnalysisResultScreen(
                errorLog = selectedErrorLog!!,
                analysis = selectedAnalysis,
                apiKey = apiKey,
                onBack = {
                    showAnalysisResult = false
                    selectedErrorLog = null
                    selectedAnalysis = null
                }
            )
        } else {
            // 기존 메인 패널
            Row(modifier = Modifier.fillMaxSize()) {
            // 좌측 사이드바 네비게이션
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(64.dp)
                    .background(Color(0xFF13161C)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                SidebarIconButton(
                    selected = sidebarTab == 0,
                    icon = "⚠",
                    color = Color(0xFFF44336),
                    onClick = { sidebarTab = if (sidebarTab == 0) null else 0 }
                )
                Spacer(modifier = Modifier.weight(1f))
            }

            // 사이드바 확장 패널
            if (sidebarTab != null) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(360.dp)
                        .background(Color(0xFF1C1F26))
                        .padding(20.dp)
                ) {
                    when (sidebarTab) {
                        0 -> ErrorLogSidebar(
                            viewModel = viewModel,
                            serverId = server?.id ?: 0,
                            errorLogs = errorLogs,
                            apiKey = apiKey,
                            onShowAnalysis = { log, analysis ->
                                selectedErrorLog = log
                                selectedAnalysis = analysis
                                showAnalysisResult = true
                            },
                            onRemoveLog = { log ->
                                // ViewModel에서 로그와 분석 결과 삭제
                                server?.id?.let { serverId ->
                                    viewModel.removeLog(serverId, log)
                                    viewModel.removeErrorAnalysis(serverId, log)
                                }
                            }
                        )
                    }
                }
            }

            // 메인 콘텐츠 영역
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFF1A1D23))
                    .padding(24.dp)
            ) {
                // 서버 상태 헤더
                ServerStatusHeader(
                    server = server,
                    isConnected = isConnected,
                    isRunning = isRunning,
                    onConnect = {
                        // Local 모드일 때는 onConnect가 시작 역할
                        if (server?.serverType == "Local") {
                            coroutineScope.launch {
                                addLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                                addLog("로컬 터미널에서 서버 시작 중...")
                                addLog("경로: ${server.logPath}")
                                addLog("명령어: ${server.startCommand}")
                                addLog("OS: ${System.getProperty("os.name")}")
                                addLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                                withContext(Dispatchers.IO) {
                                    try {
                                        val serverPath = server.logPath
                                        val startCommand = if (server.startCommand.isNotBlank()) {
                                            server.startCommand
                                        } else {
                                            "echo 서버 시작 명령어가 설정되지 않았습니다"
                                        }

                                        // Windows와 Linux에 따라 다른 명령어 구성
                                        val processBuilder = if (System.getProperty("os.name").startsWith("Windows")) {
                                            // Windows: cmd.exe with UTF-8 (chcp 65001)
                                            ProcessBuilder("cmd.exe")
                                        } else {
                                            ProcessBuilder("bash")
                                        }

                                        // 작업 디렉토리 설정
                                        processBuilder.directory(File(serverPath))
                                        processBuilder.redirectErrorStream(true)

                                        withContext(Dispatchers.Main) {
                                            addLog("프로세스 시작 중...")
                                            setRunning(true)
                                            setConnected(true)
                                        }

                                        val process = processBuilder.start()
                                        localServerProcess = process
                                        processOutputStream = process.outputStream // OutputStream 저장

                                        withContext(Dispatchers.Main) {
                                            addLog("✓ 프로세스 생성됨 (PID: ${process.pid()})")
                                        }

                                        // Windows: UTF-8 코드페이지 설정
                                        if (System.getProperty("os.name").startsWith("Windows")) {
                                            val writer = process.outputStream.bufferedWriter(Charsets.UTF_8)
                                            writer.write("chcp 65001\n")
                                            writer.flush()
                                        }

                                        // 시작 명령어 실행
                                        if (startCommand.isNotBlank()) {
                                            val writer = process.outputStream.bufferedWriter(Charsets.UTF_8)
                                            writer.write("$startCommand\n")
                                            writer.flush()
                                            withContext(Dispatchers.Main) {
                                                addLog("> $startCommand")
                                            }
                                        }

                                        // 실시간 로그 스트리밍 (별도 코루틴에서)
                                        launch(Dispatchers.IO) {
                                            try {
                                                // Windows는 MS949(CP949), Linux는 UTF-8
                                                val charset = if (System.getProperty("os.name").startsWith("Windows")) {
                                                    Charset.forName("MS949")  // Windows 기본 인코딩
                                                } else {
                                                    Charsets.UTF_8
                                                }

                                                val reader = process.inputStream.bufferedReader(charset)
                                                var lineCount = 0
                                                var line: String?

                                                // chcp 65001 출력 건너뛰기 플래그
                                                var skipChcpOutput = System.getProperty("os.name").startsWith("Windows")

                                                while (reader.readLine().also { line = it } != null) {
                                                    lineCount++
                                                    val currentLine = line!!

                                                    // chcp 65001 출력 건너뛰기
                                                    if (skipChcpOutput &&
                                                        (currentLine.contains("활성 코드 페이지") ||
                                                                currentLine.contains("Active code page") ||
                                                                currentLine.trim().isEmpty())
                                                    ) {
                                                        if (currentLine.contains("65001")) {
                                                            skipChcpOutput = false // chcp 완료
                                                        }
                                                        continue
                                                    }

                                                    withContext(Dispatchers.Main) {
                                                        addLog(currentLine)
                                                    }
                                                }

                                                withContext(Dispatchers.Main) {
                                                    addLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                                                    addLog("✓ 로컬 서버 프로세스 완료 (총 $lineCount 줄)")
                                                    addLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                                                }

                                                // 프로세스가 정상 종료됨
                                                withContext(Dispatchers.Main) {
                                                    processOutputStream?.close()
                                                    processOutputStream = null
                                                    setRunning(false)
                                                    setConnected(false)
                                                    localServerProcess = null
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    addLog("✗ 프로세스 스트리밍 오류: ${e.javaClass.simpleName}: ${e.message}")
                                                    e.printStackTrace()
                                                }
                                            }
                                        }

                                        withContext(Dispatchers.Main) {
                                            snackbarHostState.showSnackbar("로컬 서버가 시작되었습니다")
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            addLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                                            addLog("✗ 로컬 서버 시작 실패")
                                            addLog("오류 타입: ${e.javaClass.simpleName}")
                                            addLog("오류 메시지: ${e.message}")
                                            addLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                                            e.printStackTrace()
                                            snackbarHostState.showSnackbar("시작 실패: ${e.message}")
                                            setRunning(false)
                                            setConnected(false)
                                            localServerProcess = null
                                        }
                                    }
                                }
                            }
                        } else {
                            // SSH 서버 연결
                            coroutineScope.launch {
                                addLog("SSH 연결 시도 중...")
                                val result = sshClient?.connect()
                                if (result?.isSuccess == true) {
                                    setConnected(true)
                                    addLog("✓ SSH 연결 성공")
                                    snackbarHostState.showSnackbar("서버에 연결되었습니다")
                                } else {
                                    addLog("✗ SSH 연결 실패: ${result?.exceptionOrNull()?.message}")
                                    snackbarHostState.showSnackbar("연결 실패: ${result?.exceptionOrNull()?.message}")
                                }
                            }
                        }
                    },
                    onStart = {
                        // SSH 서버일 때만 사용 (연결 후 시작)
                        if (server?.serverType != "Local" && !isConnected) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("먼저 SSH 연결을 해주세요")
                            }
                            return@ServerStatusHeader
                        }

                        if (server?.serverType == "SSH") {
                            setRunning(true)
                            coroutineScope.launch {
                                addLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                                addLog("SSH 서버 시작 중...")
                                addLog("경로: ${server.logPath}")
                                addLog("명령어: ${server.startCommand}")
                                addLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                                val startCommand = if (server.startCommand.isNotBlank()) {
                                    server.startCommand
                                } else {
                                    "echo '서버 시작 명령어가 설정되지 않았습니다'"
                                }

                                // SSH로 서버 시작 명령어 실행 및 로그 스트리밍
                                sshClient?.startServerAndStreamLogs(
                                    serverPath = server.logPath,
                                    startCommand = startCommand,
                                    onLog = { log ->
                                        addLog(log)
                                    }
                                )

                                snackbarHostState.showSnackbar("서버를 시작했습니다")
                            }
                        }
                    },
                    onStop = {
                        coroutineScope.launch {
                            if (server?.serverType == "Local") {
                                // Local 모드: 프로세스 강제 종료
                                addLog("로컬 서버 중지 중...")

                                withContext(Dispatchers.IO) {
                                    try {
                                        localServerProcess?.let { process ->
                                            // 프로세스가 살아있으면 종료
                                            if (process.isAlive) {
                                                // Windows: taskkill로 프로세스 트리 전체 종료
                                                if (System.getProperty("os.name").startsWith("Windows")) {
                                                    try {
                                                        val pid = process.pid()
                                                        val killProcess = ProcessBuilder(
                                                            "taskkill",
                                                            "/F",
                                                            "/T",
                                                            "/PID",
                                                            pid.toString()
                                                        ).start()
                                                        killProcess.waitFor(5, TimeUnit.SECONDS)
                                                        addLog("✓ 프로세스 트리 종료됨 (PID: $pid)")
                                                    } catch (_: Exception) {
                                                        // taskkill 실패 시 기본 종료 방식 사용
                                                        process.destroy()
                                                    }
                                                } else {
                                                    // Linux: 정상 종료 시도
                                                    process.destroy()
                                                }

                                                // 정상 종료 대기 (3초)
                                                val terminated = process.waitFor(3, TimeUnit.SECONDS)
                                                if (!terminated) {
                                                    // 강제 종료
                                                    process.destroyForcibly()
                                                    process.waitFor(2, TimeUnit.SECONDS)
                                                    addLog("⚠ 프로세스 강제 종료됨")
                                                } else {
                                                    addLog("✓ 로컬 서버 정상 종료됨")
                                                }

                                                // 스트림과 프로세스 정리
                                                processOutputStream?.close()
                                                processOutputStream = null
                                                localServerProcess = null
                                                setConnected(false)
                                                setRunning(false)
                                            } else {
                                                addLog("프로세스가 이미 종료되었습니다")
                                                localServerProcess = null
                                                setConnected(false)
                                                setRunning(false)
                                            }
                                        } ?: run {
                                            addLog("실행 중인 프로세스가 없습니다")
                                            setConnected(false)
                                            setRunning(false)
                                        }

                                        withContext(Dispatchers.Main) {
                                            snackbarHostState.showSnackbar("로컬 서버를 중지했습니다")
                                        }
                                    } catch (e: Exception) {
                                        addLog("✗ 프로세스 종료 실패: ${e.message}")
                                        withContext(Dispatchers.Main) {
                                            snackbarHostState.showSnackbar("종료 실패: ${e.message}")
                                        }
                                        setRunning(false)
                                    }
                                }
                            } else {
                                // SSH 모드: 종료 명령 전송 (Ctrl+C)
                                addLog("서버 중지 요청...")
                                setRunning(false)

                                // SSH 세션에 종료 신호 전송 시도
                                try {
                                    addLog("SSH 서버 중지 신호 전송")
                                    snackbarHostState.showSnackbar("서버 중지 신호를 전송했습니다")
                                } catch (e: Exception) {
                                    addLog("✗ 중지 신호 전송 실패: ${e.message}")
                                    snackbarHostState.showSnackbar("중지 실패: ${e.message}")
                                }
                            }
                        }
                    },
                    onDisconnect = {
                        sshClient?.disconnect()
                        setConnected(false)
                        setRunning(false)
                        addLog("SSH 연결 종료")
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("서버 연결이 해제되었습니다")
                        }
                    }
                )


                Spacer(modifier = Modifier.height(20.dp))

                // 성능 메트릭 카드 (콘솔 위에만 표시)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MetricCard(
                        title = "CPU",
                        value = if (isConnected) "${cpuUsage.toInt()}%" else "—",
                        percentage = if (isConnected) cpuUsage else 0f,
                        color = Color(0xFF2196F3),
                        modifier = Modifier.weight(1f),
                        isConnected = isConnected
                    )
                    MetricCard(
                        title = "RAM",
                        value = if (isConnected) "${ramUsage.toInt()}%" else "—",
                        percentage = if (isConnected) ramUsage else 0f,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f),
                        isConnected = isConnected
                    )
                    MetricCard(
                        title = "NETWORK",
                        value = if (isConnected) "${netUsage.toInt()} MB/s" else "—",
                        percentage = if (isConnected) netUsage else 0f,
                        color = Color(0xFFFFC107),
                        modifier = Modifier.weight(1f),
                        isConnected = isConnected
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 콘솔 로그 영역
                ConsoleLogPanel(
                    logs = logs,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 명령어 입력 영역
                CommandInputBar(
                    value = commandInput,
                    onValueChange = { commandInput = it },
                    onExecute = {
                        if (commandInput.isNotBlank()) {
                            if (!isConnected) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("먼저 서버를 시작해주세요")
                                }
                                return@CommandInputBar
                            }

                            val cmd = commandInput
                            commandInput = ""
                            addLog("> $cmd")

                            coroutineScope.launch {
                                if (server?.serverType == "Local") {
                                    // Local 모드: 실행 중인 프로세스의 stdin에 명령 전송
                                    withContext(Dispatchers.IO) {
                                        try {
                                            processOutputStream?.let { outputStream ->
                                                val writer = outputStream.bufferedWriter(Charsets.UTF_8)
                                                writer.write("$cmd\n")
                                                writer.flush()
                                            } ?: run {
                                                withContext(Dispatchers.Main) {
                                                    addLog("✗ 프로세스가 실행 중이지 않습니다")
                                                }
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                addLog("✗ 명령 전송 실패: ${e.message}")
                                            }
                                        }
                                    }
                                } else {
                                    // SSH 모드: SSH로 명령 실행
                                    sshClient?.executeCommand(cmd) { output ->
                                        addLog(output)
                                    }
                                }
                            }
                        }
                    },
                    enabled = isConnected,
                    pointColor = pointColor
                )
            }
        } // Row 닫기 (기존 메인 패널)
        } // if-else 닫기 (분석 결과 화면 vs 메인 패널)

        // 스낵바
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
        ) {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF2C2F36),
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = data.visuals.message,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            )
        }
    }
}