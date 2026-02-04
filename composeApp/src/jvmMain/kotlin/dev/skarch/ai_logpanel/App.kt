package dev.skarch.ai_logpanel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import dev.skarch.ai_logpanel.ui.MainViewModel
import dev.skarch.ai_logpanel.ui.theme.GlassmorphismTheme
import dev.skarch.ai_logpanel.utils.ApiKeyProvider
import dev.skarch.ai_logpanel.utils.ServerStorage
import dev.skarch.ai_logpanel.ssh.SshClient
import androidx.compose.ui.window.WindowState
import dev.skarch.ai_logpanel.ui.AboutDialog
import dev.skarch.ai_logpanel.ui.MainPanelImproved
import dev.skarch.ai_logpanel.ui.MissingApiKeyScreen
import dev.skarch.ai_logpanel.ui.SettingsDialog
import dev.skarch.ai_logpanel.ui.TopBar
import kotlinx.coroutines.launch


@Composable
fun FrameWindowScope.App(
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
                                port = server.port,
                                user = server.user,
                                password = server.password,
                                privateKeyPath = server.privateKeyPath
                            )
                        } else null
                    }
                    MainPanelImproved(
                        viewModel = viewModel,
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

