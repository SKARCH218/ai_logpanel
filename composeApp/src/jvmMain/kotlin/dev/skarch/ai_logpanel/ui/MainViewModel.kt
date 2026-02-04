package dev.skarch.ai_logpanel.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.skarch.ai_logpanel.data.GeminiRepository
import dev.skarch.ai_logpanel.data.SshRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class Server(
    val id: Int,
    val name: String,
    val host: String,
    val port: Int = 22,
    val user: String,
    val password: String = "",
    val privateKeyPath: String,
    val logPath: String,
    val serverType: String = "SSH", // "SSH" or "Local"
    val startCommand: String = "", // 서버 시작 명령어
    val osType: String = "Linux", // "Linux" or "Windows"
    val logs: List<String> = emptyList(),
    val analysis: String? = null,
    val isRunning: Boolean = false, // 서버 실행 상태
    val isConnected: Boolean = false, // 연결 상태
    val errorAnalysis: Map<String, String> = emptyMap() // 에러 로그 -> AI 분석 결과 매핑
)

class MainViewModel : ViewModel() {

    private lateinit var geminiRepository: GeminiRepository
    private val sshRepository = SshRepository()

    var servers = mutableStateListOf<Server>()
        private set

    var geminiUiState by mutableStateOf<GeminiUiState>(GeminiUiState.Idle)
        private set

    fun connectAndTail(server: Server) {
        viewModelScope.launch {
            try {
                sshRepository.connect(server.user, server.host, 22, server.privateKeyPath)
                sshRepository.tailLog(server.logPath)
                    .catch { e ->
                        // Handle error
                    }
                    .collect { logLine ->
                        val currentLogs = servers.find { it.id == server.id }?.logs ?: emptyList()
                        val updatedLogs = (currentLogs + logLine).takeLast(100) // Keep last 100 lines
                        updateServer(server.id) { it.copy(logs = updatedLogs) }
                    }
            } catch (e: Exception) {
                // Handle connection error
            }
        }
    }

    fun analyzeLog(server: Server) {
        viewModelScope.launch {
            geminiUiState = GeminiUiState.Loading
            val log = server.logs.joinToString("\n")
            geminiRepository.analyzeLog(log)
                .catch { e ->
                    geminiUiState = GeminiUiState.Error(e.message ?: "알 수 없는 오류가 발생했습니다.")
                }
                .collect { response ->
                    updateServer(server.id) { it.copy(analysis = response) }
                    geminiUiState = GeminiUiState.Success(response)
                }
        }
    }

    private fun updateServer(serverId: Int, update: (Server) -> Server) {
        val index = servers.indexOfFirst { it.id == serverId }
        if (index != -1) {
            servers[index] = update(servers[index])
        }
    }

    fun setServerRunning(serverId: Int, isRunning: Boolean) {
        updateServer(serverId) { it.copy(isRunning = isRunning) }
    }

    fun setServerConnected(serverId: Int, isConnected: Boolean) {
        updateServer(serverId) { it.copy(isConnected = isConnected) }
    }

    fun addLog(serverId: Int, log: String) {
        updateServer(serverId) { server ->
            val updatedLogs = (server.logs + log).takeLast(500)
            server.copy(logs = updatedLogs)
        }
    }

    fun setLogs(serverId: Int, logs: List<String>) {
        updateServer(serverId) { it.copy(logs = logs) }
    }

    fun setErrorAnalysis(serverId: Int, errorLog: String, analysis: String) {
        updateServer(serverId) { server ->
            val updatedMap = server.errorAnalysis + (errorLog to analysis)
            server.copy(errorAnalysis = updatedMap)
        }
    }

    fun getErrorAnalysis(serverId: Int, errorLog: String): String? {
        return servers.find { it.id == serverId }?.errorAnalysis?.get(errorLog)
    }

    fun getServer(serverId: Int): Server? {
        return servers.find { it.id == serverId }
    }

    fun removeLog(serverId: Int, logToRemove: String) {
        updateServer(serverId) { server ->
            val updatedLogs = server.logs.filter { it != logToRemove }
            server.copy(logs = updatedLogs)
        }
    }

    fun removeErrorAnalysis(serverId: Int, errorLog: String) {
        updateServer(serverId) { server ->
            val updatedMap = server.errorAnalysis - errorLog
            server.copy(errorAnalysis = updatedMap)
        }
    }

    override fun onCleared() {
        super.onCleared()
        sshRepository.disconnect()
    }
}

sealed interface GeminiUiState {
    object Idle : GeminiUiState
    object Loading : GeminiUiState
    data class Success(val analysis: String) : GeminiUiState
    data class Error(val message: String) : GeminiUiState
}
