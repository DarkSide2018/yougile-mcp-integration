package com.yougile.alertbot.harness

import com.yougile.alertbot.model.Alert
import com.yougile.alertbot.model.AlertPriority
import com.yougile.alertbot.ollama.ChatRequest
import com.yougile.alertbot.ollama.ChatResponse
import com.yougile.alertbot.ollama.EmbedRequest
import com.yougile.alertbot.ollama.EmbedResponse
import com.yougile.alertbot.ollama.OllamaClient
import com.yougile.alertbot.ollama.ResponseMessage
import com.yougile.alertbot.rag.KnowledgeBaseService
import com.yougile.alertbot.rag.loader.TemplateLoader
import com.yougile.alertbot.service.OpenCodeService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.ai.document.Document
import org.springframework.ai.embedding.Embedding
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.embedding.EmbeddingRequest
import org.springframework.ai.embedding.EmbeddingResponse

class AlertProcessingHarnessTest {

    private class MockOllamaClient : OllamaClient {
        override fun chat(request: ChatRequest): ChatResponse {
            return ChatResponse(
                model = request.model,
                message = ResponseMessage(
                    role = "assistant",
                    content = """
                        {
                          "priority": "CRITICAL",
                          "category": "infrastructure",
                          "title": "Alert: High CPU on db-master-01",
                          "description": "Database CPU reached 96.4%. Immediate action required."
                        }
                    """.trimIndent()
                ),
                done = true
            )
        }

        override fun embed(request: EmbedRequest): EmbedResponse {
            val dummyVector = (1..64).map { 0.1 }
            return EmbedResponse(
                embeddings = request.input.map { dummyVector }
            )
        }
    }

    private class MockEmbeddingModel : EmbeddingModel {
        override fun call(request: EmbeddingRequest): EmbeddingResponse {
            val embeddings = request.instructions.mapIndexed { idx, _ ->
                Embedding(FloatArray(64) { 0.1f }, idx)
            }
            return EmbeddingResponse(embeddings)
        }

        override fun embed(document: Document): FloatArray = FloatArray(64) { 0.1f }
    }

    @Test
    @DisplayName("Verify end-to-end OpenCodeService analysis parsing and fallback handling")
    fun testOpenCodeAnalysisPipeline() {
        val mockClient = MockOllamaClient()
        val mockEmbeddingModel = MockEmbeddingModel()
        val templateLoader = TemplateLoader()
        val kbService = KnowledgeBaseService(mockEmbeddingModel, templateLoader)
        kbService.initialize()

        val openCodeService = OpenCodeService(
            ollamaClient = mockClient,
            knowledgeBaseService = kbService,
            modelName = "test-model"
        )

        val alert = Alert(
            text = "CRITICAL: Host db-master-01 CPU 96%",
            priority = AlertPriority.CRITICAL,
            category = "infrastructure"
        )

        val analysis = openCodeService.analyzeAlert(alert)

        assertNotNull(analysis)
        assertEquals(AlertPriority.CRITICAL, analysis.priority)
        assertEquals("infrastructure", analysis.category)
        assertTrue(analysis.title.contains("High CPU") || analysis.title.contains("db-master-01"))
        assertTrue(analysis.description.contains("96.4%"))
    }
}
