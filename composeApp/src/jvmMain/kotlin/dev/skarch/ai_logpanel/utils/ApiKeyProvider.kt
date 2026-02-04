package dev.skarch.ai_logpanel.utils

import java.util.Properties

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
