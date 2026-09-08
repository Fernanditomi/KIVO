package com.example.kivo.ai.repository

import com.example.kivo.ai.models.AiResponse
import com.example.kivo.ai.service.KivoAiService
import com.example.kivo.ai.service.MockKivoAiService

class KivoAiRepository(
    private val aiService: KivoAiService = MockKivoAiService()
) {
    suspend fun getAiResponse(query: String, context: Map<String, Any?>): AiResponse {
        return aiService.processQuery(query, context)
    }
}
