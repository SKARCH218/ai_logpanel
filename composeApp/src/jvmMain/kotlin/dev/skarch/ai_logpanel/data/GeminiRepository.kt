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
            당신은 서버 로그 분석 전문가입니다. 
            사용자가 이전에 분석받은 오류에 대해 추가 질문을 했습니다.
            
            **원본 오류 로그:**
            ```
            $originalLog
            ```
            
            **이전 분석 결과:**
            $previousAnalysis
            
            **사용자의 질문:**
            $question
            
            위 질문에 대해 자연스럽고 친절하게 답변해주세요. 
            질문이 간단한 인사("안녕", "고마워" 등)라면 짧게 응답하고,
            기술적인 질문이라면 구체적으로 설명해주세요.
            답변은 Markdown 형식을 사용하되, 과도한 형식은 피해주세요.
        """.trimIndent()

        val response = generativeModel.generateContent(
            content {
                text(prompt)
            }
        )
        emit(response.text ?: "답변을 가져오는데 실패했습니다.")
    }
}
