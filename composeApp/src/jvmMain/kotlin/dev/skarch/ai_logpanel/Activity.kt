package dev.skarch.ai_logpanel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.skarch.ai_logpanel.ui.AddServerCard
import dev.skarch.ai_logpanel.ui.FormTextField
import dev.skarch.ai_logpanel.ui.FormTextFieldLarge
import dev.skarch.ai_logpanel.ui.OSTypeButton
import dev.skarch.ai_logpanel.ui.Server
import dev.skarch.ai_logpanel.ui.ServerCard

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
    modifier: Modifier
) {
    TODO("Not yet implemented")
}