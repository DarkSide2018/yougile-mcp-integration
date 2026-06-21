package com.yougile.alertbot.ollama

import org.springframework.ai.document.Document
import org.springframework.ai.embedding.AbstractEmbeddingModel
import org.springframework.ai.embedding.Embedding
import org.springframework.ai.embedding.EmbeddingRequest
import org.springframework.ai.embedding.EmbeddingResponse
import org.springframework.ai.embedding.EmbeddingResponseMetadata
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class OllamaEmbeddingModel(
    private val client: OllamaClient,
    @Value("\${spring.ai.ollama.embedding.options.model:qwen3-embedding:8b}") private val modelName: String
) : AbstractEmbeddingModel() {

    override fun call(request: EmbeddingRequest): EmbeddingResponse {
        val response = client.embed(EmbedRequest(model = modelName, input = request.instructions))
        val embeddings = response.embeddings?.mapIndexed { index, list ->
            Embedding(list.map { it.toFloat() }.toFloatArray(), index)
        } ?: emptyList()
        return EmbeddingResponse(embeddings)
    }

    override fun embed(doc: Document): FloatArray {
        return embed(doc.text ?: "")
    }
}
