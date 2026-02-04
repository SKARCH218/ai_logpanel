package dev.skarch.ai_logpanel.utils

import dev.skarch.ai_logpanel.ui.Server

object ServerStorage {
    private val storageDir = java.io.File(System.getProperty("user.home"), ".ai-log-panel")
    private val serversFile = java.io.File(storageDir, "servers.yml")
    private val analysisFile = java.io.File(storageDir, "analysis_cache.yml")

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

    // AI 분석 결과 저장
    fun saveAnalysisResult(errorLog: String, analysis: String) {
        try {
            val yaml = org.yaml.snakeyaml.Yaml()

            // 기존 데이터 로드
            val existingData = if (analysisFile.exists()) {
                yaml.load<MutableMap<String, String>>(analysisFile.readText(Charsets.UTF_8)) ?: mutableMapOf()
            } else {
                mutableMapOf()
            }

            // 새 분석 결과 추가 (에러 로그를 키로 사용)
            existingData[errorLog] = analysis

            // 저장
            analysisFile.writeText(yaml.dump(existingData), Charsets.UTF_8)
            println("AI 분석 결과 저장 완료")
        } catch (e: Exception) {
            println("AI 분석 결과 저장 실패: ${e.message}")
            e.printStackTrace()
        }
    }

    // AI 분석 결과 로드
    fun loadAnalysisResult(errorLog: String): String? {
        return try {
            if (!analysisFile.exists()) {
                return null
            }

            val yaml = org.yaml.snakeyaml.Yaml()
            val data = yaml.load<Map<String, String>>(analysisFile.readText(Charsets.UTF_8))
            data?.get(errorLog)
        } catch (e: Exception) {
            println("AI 분석 결과 로드 실패: ${e.message}")
            null
        }
    }

    // 특정 에러 로그 분석 결과 삭제
    fun deleteAnalysisResult(errorLog: String) {
        try {
            if (!analysisFile.exists()) {
                return
            }

            val yaml = org.yaml.snakeyaml.Yaml()
            val existingData = yaml.load<MutableMap<String, String>>(analysisFile.readText(Charsets.UTF_8))

            if (existingData != null) {
                existingData.remove(errorLog)
                analysisFile.writeText(yaml.dump(existingData), Charsets.UTF_8)
                println("AI 분석 결과 삭제 완료")
            }
        } catch (e: Exception) {
            println("AI 분석 결과 삭제 실패: ${e.message}")
            e.printStackTrace()
        }
    }
}
