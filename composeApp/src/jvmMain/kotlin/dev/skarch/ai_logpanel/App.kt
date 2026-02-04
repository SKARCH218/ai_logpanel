package dev.skarch.ai_logpanel

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.skarch.ai_logpanel.ui.MainViewModel
import dev.skarch.ai_logpanel.ui.Server
import dev.skarch.ai_logpanel.ui.theme.GlassmorphismTheme
import java.util.Properties
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import kotlinx.coroutines.launch
import com.jcraft.jsch.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.nio.charset.Charset

object ApiKeyProvider {
    fun getApiKey(): String? {
        return try {
            val properties = Properties()
            val inputStream = this::class.java.getResourceAsStream("/local.properties")
            properties.load(inputStream)
            properties.getProperty("GEMINI_API_KEY")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

// D2Coding 폰트 로더
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

// 서버 저장/로드 매니저
object ServerStorage {
    private val storageDir = java.io.File(System.getProperty("user.home"), ".ai-log-panel")
    private val serversFile = java.io.File(storageDir, "servers.yml")

    init {
        // 디렉토리가 없으면 생성
        if (!storageDir.exists()) {
            storageDir.mkdirs()
            println("서버 저장 디렉토리 생성: ${storageDir.absolutePath}")
        }
    }

    fun saveServers(servers: List<Server>) {
        try {
            val yaml = org.yaml.snakeyaml.Yaml()
            val serversData = servers.map { server ->
                mapOf(
                    "id" to server.id,
                    "name" to server.name,
                    "serverType" to server.serverType,
                    "host" to server.host,
                    "port" to server.port,
                    "user" to server.user,
                    "password" to server.password,
                    "privateKeyPath" to server.privateKeyPath,
                    "logPath" to server.logPath,
                    "startCommand" to server.startCommand,
                    "osType" to server.osType
                )
            }

            serversFile.writeText(yaml.dump(serversData), Charsets.UTF_8)
            println("서버 정보 저장 완료: ${serversFile.absolutePath}")
        } catch (e: Exception) {
            println("서버 저장 실패: ${e.message}")
            e.printStackTrace()
        }
    }

    fun loadServers(): List<Server> {
        return try {
            if (!serversFile.exists()) {
                println("서버 파일이 없습니다. 빈 리스트 반환.")
                return emptyList()
            }

            val yaml = org.yaml.snakeyaml.Yaml()
            val content = serversFile.readText(Charsets.UTF_8)
            val serversData = yaml.load<List<Map<String, Any>>>(content) ?: emptyList()

            serversData.map { data ->
                Server(
                    id = (data["id"] as? Int) ?: 0,
                    name = data["name"] as? String ?: "Unknown",
                    host = data["host"] as? String ?: "",
                    port = (data["port"] as? Int) ?: 22,
                    user = data["user"] as? String ?: "",
                    password = data["password"] as? String ?: "",
                    privateKeyPath = data["privateKeyPath"] as? String ?: "",
                    logPath = data["logPath"] as? String ?: "",
                    serverType = data["serverType"] as? String ?: "SSH",
                    startCommand = data["startCommand"] as? String ?: "",
                    osType = data["osType"] as? String ?: "Linux"
                )
            }.also {
                println("서버 정보 로드 완료: ${it.size}개")
            }
        } catch (e: Exception) {
            println("서버 로드 실패: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    fun getStoragePath(): String = storageDir.absolutePath
}

// SSH 클라이언트 구현 (개선된 버전)
class SshClient(
    private val host: String,
    private val port: Int,
    private val user: String,
    private val password: String = "",
    private val privateKeyPath: String = ""
) {
    private var session: Session? = null
    var isConnected: Boolean = false
        private set

    var onError: ((String) -> Unit)? = null

    suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (session?.isConnected == true) {
                return@withContext Result.success(Unit)
            }

            val jsch = JSch()

            // 프라이빗 키가 있으면 사용
            if (privateKeyPath.isNotBlank()) {
                try {
                    jsch.addIdentity(privateKeyPath)
                } catch (e: Exception) {
                    onError?.invoke("프라이빗 키 로드 실패: ${e.message}")
                }
            }

            session = jsch.getSession(user, host, port)

            // 비밀번호가 있으면 설정
            if (password.isNotBlank()) {
                session?.setPassword(password)
            }

            session?.setConfig("StrictHostKeyChecking", "no")
            session?.connect(10000) // 10초 타임아웃

            isConnected = session?.isConnected ?: false

            if (isConnected) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("연결 실패"))
            }
        } catch (e: Exception) {
            isConnected = false
            val errorMsg = when {
                e.message?.contains("Auth fail") == true -> "인증 실패: 사용자명 또는 비밀번호를 확인하세요"
                e.message?.contains("Connection refused") == true -> "연결 거부: 서버 주소와 포트를 확인하세요"
                e.message?.contains("timeout") == true -> "연결 시간 초과: 네트워크 연결을 확인하세요"
                e.message?.contains("UnknownHostException") == true -> "호스트를 찾을 수 없음: 서버 주소를 확인하세요"
                else -> "연결 실패: ${e.message}"
            }
            onError?.invoke(errorMsg)
            Result.failure(Exception(errorMsg, e))
        }
    }

    suspend fun execCommand(cmd: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!isConnected || session?.isConnected != true) {
                return@withContext Result.failure(Exception("SSH 연결이 끊어졌습니다"))
            }

            val channel = session?.openChannel("exec") as? ChannelExec
                ?: return@withContext Result.failure(Exception("채널 생성 실패"))

            channel.setCommand(cmd)
            val input: InputStream = channel.inputStream
            val errorStream: InputStream = channel.errStream
            channel.connect(5000)

            val result = input.bufferedReader().readText()
            val error = errorStream.bufferedReader().readText()

            channel.disconnect()

            if (error.isNotBlank()) {
                Result.failure(Exception("명령 실행 오류: $error"))
            } else {
                Result.success(result)
            }
        } catch (e: Exception) {
            onError?.invoke("명령 실행 실패: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun startServerAndStreamLogs(
        serverPath: String,
        startCommand: String,
        onLog: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            if (!isConnected || session?.isConnected != true) {
                onError?.invoke("SSH 연결이 끊어졌습니다")
                return@withContext
            }

            val channel = session?.openChannel("exec") as? ChannelExec ?: return@withContext

            // 서버 폴더로 이동하고 명령 실행
            val fullCommand = if (serverPath.isNotBlank()) {
                "cd $serverPath && $startCommand"
            } else {
                startCommand
            }

            channel.setCommand(fullCommand)
            channel.setPty(true) // 가상 터미널 활성화

            val input: InputStream = channel.inputStream
            val errorStream: InputStream = channel.errStream

            channel.connect()

            // 로그 스트리밍
            launch(Dispatchers.IO) {
                input.bufferedReader().forEachLine { line ->
                    if (line.isNotBlank()) {
                        onLog(line)
                    }
                }
            }

            launch(Dispatchers.IO) {
                errorStream.bufferedReader().forEachLine { line ->
                    if (line.isNotBlank()) {
                        onLog("[ERROR] $line")
                    }
                }
            }

        } catch (e: Exception) {
            onError?.invoke("서버 시작 실패: ${e.message}")
        }
    }

    suspend fun executeCommand(cmd: String, onLog: (String) -> Unit) = withContext(Dispatchers.IO) {
        try {
            if (!isConnected || session?.isConnected != true) {
                onError?.invoke("SSH 연결이 끊어졌습니다")
                return@withContext
            }

            val channel = session?.openChannel("exec") as? ChannelExec ?: return@withContext
            channel.setCommand(cmd)
            channel.setPty(true)

            val input: InputStream = channel.inputStream
            channel.connect()

            input.bufferedReader().forEachLine { line ->
                if (line.isNotBlank()) {
                    onLog(line)
                }
            }

            channel.disconnect()
        } catch (e: Exception) {
            onError?.invoke("명령 실행 실패: ${e.message}")
        }
    }

    // 성능 데이터 가져오기 (CPU, RAM, Network)
    data class PerformanceData(val cpu: Float, val ram: Float, val network: Float)

    suspend fun getPerformanceData(): Result<PerformanceData> = withContext(Dispatchers.IO) {
        try {
            if (!isConnected || session?.isConnected != true) {
                return@withContext Result.failure(Exception("SSH 연결이 끊어졌습니다"))
            }

            var cpuUsage = 0f
            var ramUsage = 0f
            var networkUsage = 0f

            // CPU 사용률 가져오기 (Linux: top 명령어)
            try {
                val cpuChannel = session?.openChannel("exec") as? ChannelExec
                cpuChannel?.setCommand("top -bn1 | grep 'Cpu(s)' | sed 's/.*, *\\([0-9.]*\\)%* id.*/\\1/' | awk '{print 100 - \$1}'")
                cpuChannel?.connect()
                val cpuResult = cpuChannel?.inputStream?.bufferedReader()?.readText()?.trim()
                cpuChannel?.disconnect()
                cpuUsage = cpuResult?.toFloatOrNull() ?: 0f
            } catch (e: Exception) {
                // CPU 정보 가져오기 실패 시 기본값 유지
            }

            // RAM 사용률 가져오기 (Linux: free 명령어)
            try {
                val ramChannel = session?.openChannel("exec") as? ChannelExec
                ramChannel?.setCommand("free | grep Mem | awk '{print (\$3/\$2) * 100.0}'")
                ramChannel?.connect()
                val ramResult = ramChannel?.inputStream?.bufferedReader()?.readText()?.trim()
                ramChannel?.disconnect()
                ramUsage = ramResult?.toFloatOrNull() ?: 0f
            } catch (e: Exception) {
                // RAM 정보 가져오기 실패 시 기본값 유지
            }

            // 네트워크 사용량 (간단한 예시 - 실제로는 더 복잡한 로직 필요)
            try {
                val netChannel = session?.openChannel("exec") as? ChannelExec
                netChannel?.setCommand("cat /proc/net/dev | grep eth0 | awk '{print (\$2+\$10)/1024/1024}'")
                netChannel?.connect()
                val netResult = netChannel?.inputStream?.bufferedReader()?.readText()?.trim()
                netChannel?.disconnect()
                networkUsage = netResult?.toFloatOrNull() ?: 0f
            } catch (e: Exception) {
                // Network 정보 가져오기 실패 시 기본값 유지
            }

            Result.success(PerformanceData(cpuUsage, ramUsage, networkUsage))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun disconnect() {
        try {
            session?.disconnect()
            isConnected = false
        } catch (e: Exception) {
            onError?.invoke("연결 해제 실패: ${e.message}")
        }
    }
}

@Composable
fun App(
    windowState: WindowState,
    onMinimize: () -> Unit,
    onMaximize: () -> Unit,
    onClose: () -> Unit
) {
    val apiKey = remember { ApiKeyProvider.getApiKey() }
    val viewModel = remember { MainViewModel() }

    // 저장된 서버 리스트 로드
    LaunchedEffect(Unit) {
        val savedServers = ServerStorage.loadServers()
        viewModel.servers.clear()
        viewModel.servers.addAll(savedServers)
        println("저장 위치: ${ServerStorage.getStoragePath()}")
    }

    val servers = viewModel.servers
    var selectedServerId by remember { mutableStateOf<Int?>(null) }
    var showServerInput by remember { mutableStateOf(false) }
    var showServerTypeSelection by remember { mutableStateOf(false) } // SSH/Local 선택 화면
    var selectedServerType by remember { mutableStateOf<String?>(null) } // "SSH" or "Local"
    var editServerId by remember { mutableStateOf<Int?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var darkMode by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()


    GlassmorphismTheme(darkTheme = darkMode) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1D23))) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopBar(
                    onMenu = {
                        selectedServerId = null
                        showServerInput = false
                        showServerTypeSelection = false
                        selectedServerType = null
                        editServerId = null
                    },
                    onMinimize = onMinimize,
                    onMaximize = onMaximize,
                    onClose = onClose,
                    onToggleTheme = { darkMode = !darkMode },
                    onShowSettings = { showSettings = true },
                    onShowAbout = { showAbout = true },
                    darkMode = darkMode,
                    showBackButton = selectedServerId != null || showServerInput || editServerId != null || showServerTypeSelection
                )
                if (apiKey.isNullOrBlank()) {
                    MissingApiKeyScreen()
                } else if (selectedServerId == null && !showServerInput && !showServerTypeSelection && editServerId == null) {
                    // 서버 선택 메인 메뉴 화면
                    ServerSelectionScreen(
                        servers = servers,
                        onServerClick = { selectedServerId = it.id },
                        onAddServer = {
                            showServerTypeSelection = true
                        },
                        onEditServer = { editServerId = it.id },
                        onDeleteServer = { server ->
                            viewModel.servers.remove(server)
                            ServerStorage.saveServers(servers.toList())
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("서버가 삭제되었습니다.")
                            }
                        }
                    )
                } else if (showServerTypeSelection && !showServerInput) {
                    // SSH/Local 선택 화면
                    ServerTypeSelectionScreen(
                        onSelectSSH = {
                            selectedServerType = "SSH"
                            showServerTypeSelection = false
                            showServerInput = true
                        },
                        onSelectLocal = {
                            selectedServerType = "Local"
                            showServerTypeSelection = false
                            showServerInput = true
                        },
                        onCancel = {
                            showServerTypeSelection = false
                            selectedServerType = null
                        }
                    )
                } else if (showServerInput || editServerId != null) {
                    // 서버 입력/수정 폼
                    ServerInputFormImproved(
                        isEdit = editServerId != null,
                        serverType = selectedServerType ?: "SSH",
                        server = servers.find { it.id == editServerId },
                        onSubmit = { server ->
                            // 검증 로직
                            val isValid = if (selectedServerType == "Local") {
                                server.name.isNotBlank() && server.logPath.isNotBlank()
                            } else {
                                server.name.isNotBlank() && server.host.isNotBlank() && server.user.isNotBlank() && server.logPath.isNotBlank()
                            }

                            if (!isValid) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("필수 정보를 입력하세요.")
                                }
                                return@ServerInputFormImproved
                            }
                            if (editServerId != null) {
                                val idx = servers.indexOfFirst { it.id == editServerId }
                                if (idx >= 0) servers[idx] = server.copy(id = editServerId!!)
                                ServerStorage.saveServers(servers.toList())
                                editServerId = null
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("서버 정보가 수정되었습니다.")
                                }
                            } else {
                                viewModel.servers.add(server.copy(id = (servers.maxOfOrNull { it.id } ?: 0) + 1))
                                ServerStorage.saveServers(servers.toList())
                                showServerInput = false
                                selectedServerType = null
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("서버가 추가되었습니다.")
                                }
                            }
                        },
                        onCancel = {
                            showServerInput = false
                            showServerTypeSelection = false
                            selectedServerType = null
                            editServerId = null
                        }
                    )
                } else {
                    // 서버 패널 화면
                    val server = servers.firstOrNull { it.id == selectedServerId }
                    // SSH 클라이언트 인스턴스 준비
                    val sshClient = remember(server?.id) {
                        if (server != null) {
                            SshClient(
                                host = server.host,
                                port = 22,
                                user = server.user,
                                password = "", // 추후 서버 모델에 password 필드 추가 시 사용
                                privateKeyPath = server.privateKeyPath
                            )
                        } else null
                    }
                    MainPanelImproved(
                        server = server,
                        sshClient = sshClient,
                        onConnect = { /* SSH 클라이언트가 내부적으로 처리 */ },
                        onExec = { "" /* SSH 클라이언트가 내부적으로 처리 */ },
                        onDisconnect = { /* SSH 클라이언트가 내부적으로 처리 */ },
                        apiKey = apiKey ?: ""
                    )
                }
            }
            if (showSettings) {
                SettingsDialog(onDismiss = { showSettings = false })
            }
            if (showAbout) {
                AboutDialog(onDismiss = { showAbout = false })
            }

            // 스낵바를 오버레이로 배치 (UI를 밀지 않음)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 60.dp)
            ) {
                SnackbarHost(
                    hostState = snackbarHostState,
                    snackbar = { data ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1F2937),
                            shadowElevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = data.visuals.message,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

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

@Composable
fun WindowControlButton(icon: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Text(icon, color = color, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun MainPanel(
    server: Server,
    viewModel: MainViewModel,
    onMenu: () -> Unit
) {
    var sidebarExpanded by remember { mutableStateOf(false) }
    var sidebarTab by remember { mutableStateOf(0) } // 0: 에러, 1: 성능
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF232526))) {
        Row(modifier = Modifier.fillMaxSize()) {
            // 메인 로그 패널 (아일랜드 느낌의 블랙 글래스 카드)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(32.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0xCC18191C))
                    .shadow(24.dp, RoundedCornerShape(32.dp))
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
                    Text("서버 로그", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xAA232526))
                            .padding(16.dp)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(server.logs) { log ->
                                Text(log, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                            }
                        }
                    }
                }
            }
            // 사이드바 (아일랜드 느낌, 세로 아이콘)
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(if (sidebarExpanded) 320.dp else 72.dp)
                    .padding(vertical = 32.dp, horizontal = 0.dp)
                    .clip(RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp))
                    .background(Color(0xCC18191C))
                    .shadow(24.dp, RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(onClick = {
                    sidebarTab = 0; sidebarExpanded = !sidebarExpanded
                }) {
                    Text("❗", fontSize = 28.sp, color = if (sidebarTab == 0) Color(0xFF4A90E2) else Color.White)
                }
                Spacer(modifier = Modifier.height(16.dp))
                IconButton(onClick = {
                    sidebarTab = 1; sidebarExpanded = !sidebarExpanded
                }) {
                    Text("⚡", fontSize = 28.sp, color = if (sidebarTab == 1) Color(0xFF4A90E2) else Color.White)
                }
                Spacer(modifier = Modifier.weight(1f))
                // 확장 영역
                if (sidebarExpanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(2f)
                            .clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
                            .background(Color(0xEE232526))
                            .padding(24.dp)
                    ) {
                        if (sidebarTab == 0) {
                            Column {
                                Text("에러 로그", color = Color.White, style = MaterialTheme.typography.titleLarge)
                                // TODO: 에러 로그 카드 리스트
                            }
                        } else {
                            Column {
                                Text("성능 정보", color = Color.White, style = MaterialTheme.typography.titleLarge)
                                // TODO: 성능 정보 표시
                            }
                        }
                    }
                }
            }
        }
        // TopBar 메뉴 버튼에서 메인으로 돌아가기 지원
        Box(modifier = Modifier.align(Alignment.TopStart)) {
            // 이미 TopBar에서 메뉴 버튼 구현
        }
    }
    LaunchedEffect(server.id) {
        viewModel.connectAndTail(server)
    }
}

@Composable
fun MissingApiKeyScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF1A1D23)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFF252932),
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "⚠️",
                    fontSize = 64.sp
                )
                Text(
                    "API 키가 필요합니다",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "local.properties 파일에 GEMINI_API_KEY를 추가해주세요.",
                    color = Color(0xFF9CA3AF),
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

fun main() = application {
    val windowState = rememberWindowState(placement = WindowPlacement.Floating, position = WindowPosition.Aligned(Alignment.Center))
    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "AI Log Panel",
        undecorated = true,
        transparent = true,
        resizable = true
    ) {
        // 최소 크기 지정 (Awt Window 접근)
        val window = this.window
        LaunchedEffect(Unit) {
            window.minimumSize = java.awt.Dimension(1200, 800)
        }
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

// 개선된 서버 카드 (호버, 삭제, 수정, 선택 강조)
@Composable
fun ServerCardImproved(
    server: Server,
    selected: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var hovered by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .width(220.dp)
            .height(120.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (selected) Color(0xFF2A2D34) else if (hovered) Color(0xFF232526) else Color(0xFF1C1D20)
            )
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) Color(0xFF4A90E2) else Color(0xFF44474A),
                shape = RoundedCornerShape(24.dp)
            )
            // .hoverable(interactionSource = remember { MutableInteractionSource() }) // 미사용, 오류 방지 위해 주석처리
            .clickable { onClick() }
            .padding(16.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(server.name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.weight(1f))
                // 아이콘 부분은 임시로 텍스트로 대체 (아이콘 오류 해결 후 복구)
                Text("수정", color = Color(0xFF4A90E2), modifier = Modifier.size(18.dp).clickable { onEdit() })
                Spacer(modifier = Modifier.width(8.dp))
                Text("삭제", color = Color(0xFFF44336), modifier = Modifier.size(18.dp).clickable { onDelete() })
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(server.host, color = Color(0xFFB0B0B0), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(4.dp))
            Text("유저: ${server.user}", color = Color(0xFFB0B0B0), fontSize = 13.sp)
        }
    }
}

// 서버 추가 카드 개선
@Composable
fun AddServerCardImproved(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(220.dp)
            .height(120.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF232526))
            .border(1.dp, Color(0xFF4A90E2), RoundedCornerShape(24.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text("+ 서버 추가", color = Color(0xFF4A90E2), fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

// ServerInputForm 개선: Server 생성자에 맞게 파라미터 전달 및 관련 오류 수정
@Composable
fun ServerInputForm(
    isEdit: Boolean,
    server: Server?,
    onSubmit: (Server) -> Unit,
    onCancel: () -> Unit,
    darkMode: Boolean
) {
    var name by remember { mutableStateOf(server?.name ?: "") }
    var host by remember { mutableStateOf(server?.host ?: "") }
    var port by remember { mutableStateOf(server?.port?.toString() ?: "22") }
    var user by remember { mutableStateOf(server?.user ?: "") }
    var password by remember { mutableStateOf(server?.password ?: "") }
    var privateKeyPath by remember { mutableStateOf(server?.privateKeyPath ?: "") }
    var logPath by remember { mutableStateOf(server?.logPath ?: "") }
    var serverType by remember { mutableStateOf(server?.serverType ?: "SSH") }
    var startCommand by remember { mutableStateOf(server?.startCommand ?: "") }
    var osType by remember { mutableStateOf(server?.osType ?: "Linux") }
    val pointColor = Color(0xFF2196F3) // 파란색 계열
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (darkMode) Color(0xFF232526) else Color(0xFFF5F5F5))
            .padding(32.dp)
    ) {
        Text(if (isEdit) "서버 수정" else "서버 추가", color = pointColor, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(Modifier.weight(1f)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("이름") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = pointColor, focusedLabelColor = pointColor)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("호스트") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = pointColor, focusedLabelColor = pointColor)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = user,
                    onValueChange = { user = it },
                    label = { Text("유저") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = pointColor, focusedLabelColor = pointColor)
                )
            }
            Column(Modifier.weight(1f)) {
                OutlinedTextField(
                    value = privateKeyPath,
                    onValueChange = { privateKeyPath = it },
                    label = { Text("키 경로") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = pointColor, focusedLabelColor = pointColor)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = logPath,
                    onValueChange = { logPath = it },
                    label = { Text("로그 경로") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = pointColor, focusedLabelColor = pointColor)
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            OutlinedButton(onClick = onCancel, border = androidx.compose.foundation.BorderStroke(1.dp, pointColor)) {
                Text("취소", color = pointColor)
            }
            Spacer(modifier = Modifier.width(16.dp))
            OutlinedButton(
                onClick = {
                    onSubmit(
                        Server(
                            id = server?.id ?: 0,
                            name = name,
                            host = host,
                            port = port.toIntOrNull() ?: 22,
                            user = user,
                            password = password,
                            privateKeyPath = privateKeyPath,
                            logPath = logPath,
                            serverType = serverType,
                            startCommand = startCommand,
                            osType = osType
                        )
                    )
                },
                border = androidx.compose.foundation.BorderStroke(1.5.dp, pointColor)
            ) {
                Text("확인", color = pointColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 전문 서버 관리 대시보드 스타일 패널
@Composable
fun MainPanelImproved(
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

    // 로그 상태 관리
    var logs by remember { mutableStateOf(server?.logs?.toMutableList() ?: mutableListOf<String>()) }
    val errorLogs = remember(logs) {
        logs.filter { it.contains("error", true) || it.contains("fail", true) || it.contains("[ERROR]", true) }
    }

    // 성능 데이터
    var cpuUsage by remember { mutableStateOf(0f) }
    var ramUsage by remember { mutableStateOf(0f) }
    var netUsage by remember { mutableStateOf(0f) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var isConnected by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(false) }
    var connectionError by remember { mutableStateOf<String?>(null) }

    // 로컬 서버 프로세스 및 입출력 스트림 저장
    var localServerProcess by remember { mutableStateOf<Process?>(null) }
    var processOutputStream by remember { mutableStateOf<java.io.OutputStream?>(null) }

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
                kotlinx.coroutines.delay(5000) // 5초 대기
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
                    if (!process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)) {
                        process.destroyForcibly()
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1D23))) {
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
                        0 -> ErrorLogSidebar(errorLogs, apiKey)
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
                                logs.add("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                                logs.add("로컬 터미널에서 서버 시작 중...")
                                logs.add("경로: ${server.logPath}")
                                logs.add("명령어: ${server.startCommand}")
                                logs.add("OS: ${System.getProperty("os.name")}")
                                logs.add("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

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
                                        processBuilder.directory(java.io.File(serverPath))
                                        processBuilder.redirectErrorStream(true)

                                        withContext(Dispatchers.Main) {
                                            logs.add("프로세스 시작 중...")
                                            isRunning = true
                                            isConnected = true
                                        }

                                        val process = processBuilder.start()
                                        localServerProcess = process
                                        processOutputStream = process.outputStream // OutputStream 저장

                                        withContext(Dispatchers.Main) {
                                            logs.add("✓ 프로세스 생성됨 (PID: ${process.pid()})")
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
                                                logs.add("> $startCommand")
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
                                                         currentLine.trim().isEmpty())) {
                                                        if (currentLine.contains("65001")) {
                                                            skipChcpOutput = false // chcp 완료
                                                        }
                                                        continue
                                                    }

                                                    withContext(Dispatchers.Main) {
                                                        logs = (logs + currentLine).takeLast(500).toMutableList()
                                                    }
                                                }

                                                withContext(Dispatchers.Main) {
                                                    logs.add("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                                                    logs.add("✓ 로컬 서버 프로세스 완료 (총 $lineCount 줄)")
                                                    logs.add("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                                                }

                                                // 프로세스가 정상 종료됨
                                                withContext(Dispatchers.Main) {
                                                    processOutputStream?.close()
                                                    processOutputStream = null
                                                    isRunning = false
                                                    isConnected = false
                                                    localServerProcess = null
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    logs.add("✗ 프로세스 스트리밍 오류: ${e.javaClass.simpleName}: ${e.message}")
                                                    e.printStackTrace()
                                                }
                                            }
                                        }

                                        withContext(Dispatchers.Main) {
                                            snackbarHostState.showSnackbar("로컬 서버가 시작되었습니다")
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            logs.add("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                                            logs.add("✗ 로컬 서버 시작 실패")
                                            logs.add("오류 타입: ${e.javaClass.simpleName}")
                                            logs.add("오류 메시지: ${e.message}")
                                            logs.add("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                                            e.printStackTrace()
                                            snackbarHostState.showSnackbar("시작 실패: ${e.message}")
                                            isRunning = false
                                            isConnected = false
                                            localServerProcess = null
                                        }
                                    }
                                }
                            }
                        } else {
                            // SSH 서버 연결
                            coroutineScope.launch {
                                logs.add("SSH 연결 시도 중...")
                                val result = sshClient?.connect()
                                if (result?.isSuccess == true) {
                                    isConnected = true
                                    logs.add("✓ SSH 연결 성공")
                                    snackbarHostState.showSnackbar("서버에 연결되었습니다")
                                } else {
                                    logs.add("✗ SSH 연결 실패: ${result?.exceptionOrNull()?.message}")
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
                            isRunning = true
                            coroutineScope.launch {
                                logs.add("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                                logs.add("SSH 서버 시작 중...")
                                logs.add("경로: ${server.logPath}")
                                logs.add("명령어: ${server.startCommand}")
                                logs.add("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

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
                                        logs = (logs + log).takeLast(500).toMutableList()
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
                                logs.add("로컬 서버 중지 중...")

                                withContext(Dispatchers.IO) {
                                    try {
                                        localServerProcess?.let { process ->
                                            // 프로세스가 살아있으면 종료
                                            if (process.isAlive) {
                                                // Windows: taskkill로 프로세스 트리 전체 종료
                                                if (System.getProperty("os.name").startsWith("Windows")) {
                                                    try {
                                                        val pid = process.pid()
                                                        val killProcess = ProcessBuilder("taskkill", "/F", "/T", "/PID", pid.toString()).start()
                                                        killProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)
                                                        logs.add("✓ 프로세스 트리 종료됨 (PID: $pid)")
                                                    } catch (e: Exception) {
                                                        // taskkill 실패 시 기본 종료 방식 사용
                                                        process.destroy()
                                                    }
                                                } else {
                                                    // Linux: 정상 종료 시도
                                                    process.destroy()
                                                }

                                                // 정상 종료 대기 (3초)
                                                val terminated = process.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)
                                                if (!terminated) {
                                                    // 강제 종료
                                                    process.destroyForcibly()
                                                    process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)
                                                    logs.add("⚠ 프로세스 강제 종료됨")
                                                } else {
                                                    logs.add("✓ 로컬 서버 정상 종료됨")
                                                }

                                                // 스트림과 프로세스 정리
                                                processOutputStream?.close()
                                                processOutputStream = null
                                                localServerProcess = null
                                                isConnected = false
                                                isRunning = false
                                            } else {
                                                logs.add("프로세스가 이미 종료되었습니다")
                                                localServerProcess = null
                                                isConnected = false
                                                isRunning = false
                                            }
                                        } ?: run {
                                            logs.add("실행 중인 프로세스가 없습니다")
                                            isConnected = false
                                            isRunning = false
                                        }

                                        withContext(Dispatchers.Main) {
                                            snackbarHostState.showSnackbar("로컬 서버를 중지했습니다")
                                        }
                                    } catch (e: Exception) {
                                        logs.add("✗ 프로세스 종료 실패: ${e.message}")
                                        withContext(Dispatchers.Main) {
                                            snackbarHostState.showSnackbar("종료 실패: ${e.message}")
                                        }
                                        isRunning = false
                                    }
                                }
                            } else {
                                // SSH 모드: 종료 명령 전송 (Ctrl+C)
                                logs.add("서버 중지 요청...")
                                isRunning = false

                                // SSH 세션에 종료 신호 전송 시도
                                try {
                                    // TODO: SSH 세션에 인터럽트 신호 전송
                                    logs.add("SSH 서버 중지 신호 전송")
                                    snackbarHostState.showSnackbar("서버 중지 신호를 전송했습니다")
                                } catch (e: Exception) {
                                    logs.add("✗ 중지 신호 전송 실패: ${e.message}")
                                    snackbarHostState.showSnackbar("중지 실패: ${e.message}")
                                }
                            }
                        }
                    },
                    onDisconnect = {
                        sshClient?.disconnect()
                        isConnected = false
                        isRunning = false
                        logs.add("SSH 연결 종료")
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
                            logs.add("> $cmd")

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
                                                    logs.add("✗ 프로세스가 실행 중이지 않습니다")
                                                }
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                logs.add("✗ 명령 전송 실패: ${e.message}")
                                            }
                                        }
                                    }
                                } else {
                                    // SSH 모드: SSH로 명령 실행
                                    sshClient?.executeCommand(cmd) { output ->
                                        logs = (logs + output).takeLast(500).toMutableList()
                                    }
                                }
                            }
                        }
                    },
                    enabled = isConnected,
                    pointColor = pointColor
                )
            }
        }

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

// 사이드바 아이콘 버튼
@Composable
fun SidebarIconButton(
    selected: Boolean,
    icon: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) color.copy(alpha = 0.2f) else Color.Transparent)
            .clickable { onClick() }
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) color else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(icon, fontSize = 24.sp, color = if (selected) color else Color(0xFFB0B0B0))
    }
}

// 에러 로그 사이드바 (오류/경고 색상, 스크롤바)
@Composable
fun ErrorLogSidebar(errorLogs: List<String>, apiKey: String) {
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
                        ErrorLogCard(log, apiKey)
                    }
                }

                // 스크롤바
                androidx.compose.foundation.VerticalScrollbar(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight(),
                    adapter = rememberScrollbarAdapter(listState),
                    style = androidx.compose.foundation.ScrollbarStyle(
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

// 에러 로그 카드 (오류/경고 색상)
@Composable
fun ErrorLogCard(log: String, apiKey: String) {
    val logLower = log.lowercase()
    val isError = logLower.contains("error") || logLower.contains("✗") || logLower.contains("[error]")
    val isWarning = logLower.contains("warn") || logLower.contains("warning") || logLower.contains("[warn]")

    var aiAnalysis by remember { mutableStateOf<String?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // 자동으로 AI 분석 시작
    LaunchedEffect(log) {
        isAnalyzing = true
        try {
            val geminiRepository = dev.skarch.ai_logpanel.data.GeminiRepository(apiKey)
            geminiRepository.analyzeLog(log).collect { analysis ->
                aiAnalysis = analysis
                isAnalyzing = false
            }
        } catch (e: Exception) {
            aiAnalysis = "분석 실패: ${e.message}"
            isAnalyzing = false
        }
    }

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
            Row(verticalAlignment = Alignment.Top) {
                Text(icon, fontSize = 18.sp, color = iconColor)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        log,
                        color = if (isWarning) Color(0xFFFFC107) else Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        fontFamily = FontLoader.d2CodingFontFamily
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (isAnalyzing) {
                            "AI 분석: 원인 분석 중..."
                        } else {
                            "AI 분석: ${aiAnalysis ?: "분석 대기 중..."}"
                        },
                        color = Color(0xFF90CAF9),
                        fontSize = 12.sp,
                        fontFamily = FontLoader.d2CodingFontFamily
                    )
                }
            }
        }
    }
}

// 성능 사이드바
// 성능 사이드바
@Composable
fun PerformanceSidebar(cpu: Float, ram: Float, net: Float, isConnected: Boolean = true) {
    Column(Modifier.fillMaxSize()) {
        Text(
            "시스템 성능",
            color = Color(0xFF4CAF50),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (isConnected) {
            PerformanceMetricCard("CPU 사용률", cpu, Color(0xFF2196F3))
            Spacer(modifier = Modifier.height(16.dp))
            PerformanceMetricCard("메모리 사용률", ram, Color(0xFF4CAF50))
            Spacer(modifier = Modifier.height(16.dp))
            PerformanceMetricCard("네트워크", net, Color(0xFFFFC107))
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "서버에 연결되어있지 않습니다",
                    color = Color(0xFF6B7280),
                    fontSize = 15.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

// 성능 메트릭 카드
@Composable
fun PerformanceMetricCard(title: String, value: Float, color: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF252932),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(title, color = Color(0xFF9CA3AF), fontSize = 13.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "${value.toInt()}%",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1F2937))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(value / 100f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color)
                )
            }
        }
    }
}

// 서버 상태 헤더
@Composable
fun ServerStatusHeader(
    server: Server?,
    isConnected: Boolean,
    isRunning: Boolean,
    onConnect: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onDisconnect: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF252932),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    server?.name ?: "서버 없음",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) Color(0xFF4CAF50) else Color(0xFF6B7280))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isConnected) "연결됨" else "연결 안됨",
                        color = Color(0xFF9CA3AF),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("•", color = Color(0xFF6B7280))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        server?.host ?: "",
                        color = Color(0xFF9CA3AF),
                        fontSize = 14.sp
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Local 서버일 때는 "시작" 버튼만 표시, SSH일 때는 "연결" 후 "시작"
                if (server?.serverType == "Local") {
                    // Local 모드: 시작/중지 버튼만
                    if (!isRunning) {
                        OutlinedButton(
                            onClick = onConnect, // Local에서는 onConnect가 시작 역할
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4CAF50)),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF4CAF50)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("시작", fontWeight = FontWeight.Medium)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onStop,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF44336)),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF44336)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("중지", fontWeight = FontWeight.Medium)
                        }
                    }
                } else {
                    // SSH 모드: 연결 → 시작 → 중지 흐름
                    if (!isConnected) {
                        OutlinedButton(
                            onClick = onConnect,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2196F3)),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF2196F3)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("연결", fontWeight = FontWeight.Medium)
                        }
                    } else {
                        if (!isRunning) {
                            OutlinedButton(
                                onClick = onStart,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4CAF50)),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF4CAF50)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("시작", fontWeight = FontWeight.Medium)
                            }
                        } else {
                            OutlinedButton(
                                onClick = onStop,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF44336)),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF44336)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("중지", fontWeight = FontWeight.Medium)
                            }
                        }
                        OutlinedButton(
                            onClick = onDisconnect,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6B7280)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6B7280)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("연결 해제", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

// 메트릭 카드
@Composable
fun MetricCard(
    title: String,
    value: String,
    percentage: Float,
    color: Color,
    modifier: Modifier = Modifier,
    isConnected: Boolean = true
) {
    Surface(
        modifier = modifier.height(120.dp),
        color = Color(0xFF252932),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            if (isConnected) {
                Column {
                    Text(title, color = Color(0xFF9CA3AF), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        value,
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF1F2937))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(percentage / 100f)
                            .clip(RoundedCornerShape(2.dp))
                            .background(color)
                    )
                }
            } else {
                // 연결 전 상태
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(title, color = Color(0xFF9CA3AF), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "연결되어있지 않음",
                        color = Color(0xFF6B7280),
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
    var lastLogCount by remember { mutableStateOf(logs.size) }

    // 로그가 추가될 때마다 자동 스크롤 (사용자가 스크롤하지 않은 경우)
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty() && !isUserScrolling) {
            listState.animateScrollToItem(logs.size - 1)
        }
        lastLogCount = logs.size
    }

    // 스크롤 상태 감지
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            // 사용자가 맨 아래가 아닌 곳으로 스크롤하면 자동 스크롤 중지
            val isAtBottom = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == logs.size - 1
            if (!isAtBottom) {
                isUserScrolling = true
            }
        }
    }

    // 맨 아래로 이동하면 자동 스크롤 재개
    LaunchedEffect(listState.firstVisibleItemIndex, listState.layoutInfo.totalItemsCount) {
        if (logs.isNotEmpty()) {
            val isAtBottom = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == logs.size - 1
            if (isAtBottom) {
                isUserScrolling = false
            }
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
                                        listState.animateScrollToItem(logs.size - 1)
                                    }
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2196F3)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2196F3)),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("맨 아래로", fontSize = 12.sp)
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

                // 스크롤바
                androidx.compose.foundation.VerticalScrollbar(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(vertical = 16.dp),
                    adapter = rememberScrollbarAdapter(listState),
                    style = androidx.compose.foundation.ScrollbarStyle(
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

// 명령어 입력 바
@Composable
fun CommandInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onExecute: () -> Unit,
    enabled: Boolean,
    pointColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF252932),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .onKeyEvent { event ->
                        if (event.key == Key.Enter &&
                            event.type == KeyEventType.KeyUp &&
                            enabled &&
                            value.isNotBlank()) {
                            onExecute()
                            true
                        } else {
                            false
                        }
                    },
                placeholder = { Text("명령어 입력...", color = Color(0xFF6B7280)) },
                singleLine = true,
                enabled = enabled,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = pointColor,
                    unfocusedBorderColor = Color(0xFF374151),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    disabledTextColor = Color(0xFF6B7280),
                    disabledBorderColor = Color(0xFF374151)
                ),
                shape = RoundedCornerShape(8.dp)
            )
            OutlinedButton(
                onClick = onExecute,
                enabled = enabled && value.isNotBlank(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = pointColor,
                    disabledContentColor = Color(0xFF6B7280)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    if (enabled) pointColor else Color(0xFF374151)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("실행", fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun PerformanceBar(label: String, percent: Float, color: Color) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .height(18.dp)
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1A1A1A))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(percent / 100f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("${percent.toInt()}%", color = color, fontWeight = FontWeight.Bold)
        }
    }
}

// 설정 다이얼로그
@Composable
fun SettingsDialog(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x88000000))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(500.dp)
                .clickable(enabled = false) {},
            color = Color(0xFF252932),
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 24.dp
        ) {
            Column(modifier = Modifier.padding(32.dp)) {
                Text(
                    "설정",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "설정 옵션은 추후 추가될 예정입니다.",
                    color = Color(0xFF9CA3AF),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("닫기", modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}

// 정보 다이얼로그
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x88000000))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(500.dp)
                .clickable(enabled = false) {},
            color = Color(0xFF252932),
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "🤖",
                    fontSize = 64.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "AI Log Panel",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "버전 1.0.0",
                    color = Color(0xFF9CA3AF),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "AI 기반 서버 로그 관리 및 분석 도구",
                    color = Color(0xFF9CA3AF),
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("닫기", modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SidebarTabButton(selected: Boolean, icon: String, description: String, pointColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) pointColor.copy(alpha = 0.15f) else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(icon, fontSize = 22.sp, color = if (selected) pointColor else Color(0xFFB0B0B0))
    }
}

@Composable
fun TopSnackbarHost(snackbarHostState: SnackbarHostState, pointColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color.Black.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.fillMaxWidth(),
            snackbar = { data ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(data.visuals.message, color = Color.White)
                    data.visuals.actionLabel?.let { label ->
                        Spacer(Modifier.width(8.dp))
                        Text(label, color = pointColor)
                    }
                }
            }
        )
    }
}

// 아웃라인 버튼, 파란색 포인트 컬러, 아일랜드 카드 스타일 적용 예시
@Composable
fun PanelIsland(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(28.dp)).shadow(16.dp, RoundedCornerShape(28.dp)),
        color = Color(0xF018191C),
        tonalElevation = 2.dp
    ) {
        Column(Modifier.padding(24.dp)) {
            Text(title, color = Color(0xFF2196F3), fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

// SSH/Local 선택 화면
@Composable
fun ServerTypeSelectionScreen(
    onSelectSSH: () -> Unit,
    onSelectLocal: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "서버 유형 선택",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "어떤 방식으로 서버를 관리하시겠습니까?",
            color = Color(0xFF9CA3AF),
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(64.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally)
        ) {
            // SSH 서버 카드
            ServerTypeCard(
                icon = "🌐",
                title = "SSH 서버",
                description = "원격 서버에 SSH로 연결하여 관리합니다",
                features = listOf(
                    "외부 서버 접속",
                    "SSH 키 또는 비밀번호 인증",
                    "원격 명령 실행"
                ),
                onClick = onSelectSSH,
                modifier = Modifier.weight(1f).heightIn(max = 400.dp)
            )

            // Local 서버 카드
            ServerTypeCard(
                icon = "💻",
                title = "로컬 서버",
                description = "이 컴퓨터에서 직접 서버를 실행합니다",
                features = listOf(
                    "로컬 프로세스 실행",
                    "빠른 접근",
                    "간편한 설정"
                ),
                onClick = onSelectLocal,
                modifier = Modifier.weight(1f).heightIn(max = 400.dp)
            )
        }
    }
}

@Composable
fun ServerTypeCard(
    icon: String,
    title: String,
    description: String,
    features: List<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clickable { onClick() },
        color = Color(0xFF252932),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                icon,
                fontSize = 72.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                description,
                color = Color(0xFF9CA3AF),
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                features.forEach { feature ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text("✓", color = Color(0xFF2196F3), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(feature, color = Color(0xFFE6EDF3), fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "선택",
                    modifier = Modifier.padding(vertical = 8.dp),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Preview
@Composable
fun MainPanelImprovedPreview() {
    GlassmorphismTheme(darkTheme = true) {
        val dummyServer = Server(
            id = 1,
            name = "테스트 서버",
            host = "127.0.0.1",
            port = 22,
            user = "root",
            password = "",
            privateKeyPath = "",
            logPath = "/var/log/syslog",
            serverType = "SSH",
            startCommand = "npm start",
            osType = "Linux",
            logs = listOf(
                "서버가 시작되었습니다.",
                "CPU 사용량: 45%",
                "에러: 연결 실패",
                "RAM 사용량: 60%",
                "네트워크: 10MB/s"
            )
        )
        MainPanelImproved(
            server = dummyServer,
            sshClient = null,
            onConnect = {},
            onExec = { "" },
            onDisconnect = {},
            apiKey = "test-api-key"
        )
    }
}

// 서버 선택 메인 화면 (일관된 디자인)
@Composable
fun ServerSelectionScreen(
    servers: List<Server>,
    onServerClick: (Server) -> Unit,
    onAddServer: () -> Unit,
    onEditServer: (Server) -> Unit,
    onDeleteServer: (Server) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Text(
            "서버를 선택하세요",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(32.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 200.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(servers) { server ->
                ServerCard(
                    server = server,
                    onClick = { onServerClick(server) },
                    onEdit = { onEditServer(server) },
                    onDelete = { onDeleteServer(server) }
                )
            }

            item {
                AddServerCard(onClick = onAddServer)
            }
        }
    }
}

// 개선된 서버 카드
@Composable
fun ServerCard(
    server: Server,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable { onClick() },
        color = Color(0xFF252932),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 4.dp
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            server.name,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            server.host,
                            color = Color(0xFF9CA3AF),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                            Text("✏", fontSize = 14.sp, color = Color(0xFF2196F3))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                            Text("🗑", fontSize = 14.sp, color = Color(0xFFF44336))
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    InfoChip("👤 ${server.user}")
                    if (server.logPath.isNotEmpty()) {
                        InfoChip("📁 서버")
                    }
                }
            }
        }
    }
}

@Composable
fun InfoChip(text: String) {
    Surface(
        color = Color(0xFF1F2937),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = Color(0xFF9CA3AF),
            fontSize = 12.sp
        )
    }
}

// 서버 추가 카드
@Composable
fun AddServerCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable { onClick() },
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF2196F3))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("+", fontSize = 40.sp, color = Color(0xFF2196F3))
                Text(
                    "새 서버 추가",
                    color = Color(0xFF2196F3),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// 서버 입력 폼 (개선된 버전)
@Composable
fun ServerInputFormImproved(
    isEdit: Boolean,
    serverType: String = "SSH", // "SSH" or "Local"
    server: Server?,
    onSubmit: (Server) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(server?.name ?: "") }
    var host by remember { mutableStateOf(server?.host ?: "") }
    var user by remember { mutableStateOf(server?.user ?: "") }
    var port by remember { mutableStateOf("22") }
    var password by remember { mutableStateOf("") }
    var serverPath by remember { mutableStateOf(server?.logPath ?: "") }
    var startCmd by remember { mutableStateOf("") }
    var osType by remember { mutableStateOf(if (serverType == "Local") "Windows" else "Linux") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (isEdit) "서버 수정" else "새 서버 추가",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            if (!isEdit) {
                Surface(
                    color = Color(0xFF2196F3).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        serverType,
                        color = Color(0xFF2196F3),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            color = Color(0xFF252932),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(32.dp).verticalScroll(rememberScrollState())) {
                // OS 선택
                Text(
                    "운영 체제",
                    color = Color(0xFF9CA3AF),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OSTypeButton(
                        label = "Linux",
                        selected = osType == "Linux",
                        onClick = { osType = "Linux" },
                        modifier = Modifier.weight(1f),
                        enabled = serverType != "Local"
                    )
                    OSTypeButton(
                        label = "Windows",
                        selected = osType == "Windows",
                        onClick = { osType = "Windows" },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 서버 이름 (크게)
                FormTextFieldLarge(
                    value = name,
                    onValueChange = { name = it },
                    label = "서버 이름 *",
                    placeholder = if (serverType == "Local") "예: 로컬 개발 서버" else "예: 메인 웹 서버"
                )

                Spacer(modifier = Modifier.height(24.dp))

                // SSH 모드일 때만 호스트 주소 & 포트 표시
                if (serverType == "SSH") {
                    // 호스트 주소 & 포트
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        FormTextField(
                            value = host,
                            onValueChange = { host = it },
                            label = "호스트 주소 *",
                            placeholder = "192.168.1.100 또는 example.com",
                            modifier = Modifier.weight(2f)
                        )
                        FormTextField(
                            value = port,
                            onValueChange = { port = it },
                            label = "포트",
                            placeholder = "22",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 사용자명 & 비밀번호
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        FormTextField(
                            value = user,
                            onValueChange = { user = it },
                            label = "사용자명 *",
                            placeholder = "root 또는 ubuntu",
                            modifier = Modifier.weight(1f)
                        )
                        FormTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = "비밀번호",
                            placeholder = "선택 사항",
                            modifier = Modifier.weight(1f),
                            isPassword = true
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // 서버 폴더 경로
                FormTextField(
                    value = serverPath,
                    onValueChange = { serverPath = it },
                    label = "서버 폴더 경로 *",
                    placeholder = if (serverType == "Local") {
                        if (osType == "Windows") "C:\\Users\\user\\myapp" else "/home/user/myapp"
                    } else {
                        if (osType == "Linux") "/home/user/myapp" else "C:\\Users\\user\\myapp"
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 서버 시작 명령어
                FormTextField(
                    value = startCmd,
                    onValueChange = { startCmd = it },
                    label = "서버 시작 명령어 (Bash)",
                    placeholder = if (osType == "Linux") "./start.sh 또는 npm start" else "start.bat 또는 myapp.exe",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 버튼 영역 (우측 하단)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onCancel,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6B7280)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF6B7280)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text("취소", modifier = Modifier.padding(horizontal = 20.dp), fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = {
                    onSubmit(
                        Server(
                            id = server?.id ?: 0,
                            name = name,
                            host = if (serverType == "Local") "localhost" else host,
                            port = port.toIntOrNull() ?: 22,
                            user = if (serverType == "Local") "local" else user,
                            password = password,
                            privateKeyPath = "",
                            logPath = serverPath,
                            serverType = serverType,
                            startCommand = startCmd,
                            osType = osType,
                            logs = server?.logs ?: emptyList()
                        )
                    )
                },
                enabled = if (serverType == "Local") {
                    name.isNotBlank() && serverPath.isNotBlank()
                } else {
                    name.isNotBlank() && host.isNotBlank() && user.isNotBlank() && serverPath.isNotBlank()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3),
                    disabledContainerColor = Color(0xFF374151)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    if (isEdit) "수정 완료" else "서버 추가",
                    modifier = Modifier.padding(horizontal = 20.dp),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    isPassword: Boolean = false
) {
    Column(modifier = modifier) {
        Text(
            label,
            color = Color(0xFF9CA3AF),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    placeholder,
                    color = Color(0xFF4B5563),
                    fontSize = 14.sp
                )
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2196F3),
                unfocusedBorderColor = Color(0xFF374151),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFF2196F3),
                focusedPlaceholderColor = Color(0xFF4B5563),
                unfocusedPlaceholderColor = Color(0xFF4B5563)
            ),
            shape = RoundedCornerShape(10.dp),
            visualTransformation = if (isPassword) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None
        )
    }
}

@Composable
fun FormTextFieldLarge(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            label,
            color = Color(0xFF9CA3AF),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            placeholder = {
                Text(
                    placeholder,
                    color = Color(0xFF4B5563),
                    fontSize = 16.sp
                )
            },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2196F3),
                unfocusedBorderColor = Color(0xFF374151),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFF2196F3),
                focusedPlaceholderColor = Color(0xFF4B5563),
                unfocusedPlaceholderColor = Color(0xFF4B5563)
            ),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
fun OSTypeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Surface(
        modifier = modifier
            .height(56.dp)
            .clickable(enabled = enabled) { onClick() },
        color = if (!enabled) Color(0xFF1A1D23)
               else if (selected) Color(0xFF2196F3).copy(alpha = 0.15f)
               else Color(0xFF1F2937),
        shape = RoundedCornerShape(12.dp),
        border = if (selected && enabled)
            androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF2196F3))
        else
            androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF374151))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                color = if (!enabled) Color(0xFF4B5563)
                       else if (selected) Color(0xFF2196F3)
                       else Color(0xFF9CA3AF),
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            )
        }
    }
}

