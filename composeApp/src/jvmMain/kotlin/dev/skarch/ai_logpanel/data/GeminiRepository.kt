package dev.skarch.ai_logpanel.data

import dev.shreyaspatil.ai.client.generativeai.GenerativeModel
import dev.shreyaspatil.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GeminiRepository(apiKey: String) {

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = apiKey
    )

    fun analyzeLog(log: String): Flow<String> = flow {
        val prompt = """
            다음 서버 로그를 분석하고, 문제의 원인과 해결책을 Markdown 형식으로 제시해주세요:
            
            ## 📋 로그 내용
            ```
            $log
            ```
            
            다음 형식으로 답변해주세요:
            
            ## 🔍 문제 분석
            [문제 설명]
            
            ## 💡 원인
            [원인 설명]
            
            ## ✅ 해결 방법
            1. [해결 방법 1]
            2. [해결 방법 2]
            3. [해결 방법 3]
        """.trimIndent()

        val response = generativeModel.generateContent(
            content {
                text(prompt)
            }
        )
        emit(response.text ?: "분석 결과를 가져오는데 실패했습니다.")
    }

    fun askFollowUpQuestion(originalLog: String, previousAnalysis: String, question: String): Flow<String> = flow {
        val prompt = """
            다음은 이전 분석 내용입니다:
            
            **원본 로그:**
            ```
            $originalLog
            ```
            
            **이전 분석:**
            $previousAnalysis
            
            **추가 질문:**
            $question
            
            위 내용을 바탕으로 질문에 답변해주세요. Markdown 형식으로 답변해주세요.
        """.trimIndent()

        val response = generativeModel.generateContent(
            content {
                text(prompt)
            }
        )
        emit(response.text ?: "답변을 가져오는데 실패했습니다.")
    }
}
