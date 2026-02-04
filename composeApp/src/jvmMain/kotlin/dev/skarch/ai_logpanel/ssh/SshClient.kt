package dev.skarch.ai_logpanel.ssh

import com.jcraft.jsch.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

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
