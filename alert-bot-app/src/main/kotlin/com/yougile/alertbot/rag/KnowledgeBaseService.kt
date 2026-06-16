package com.yougile.alertbot.rag

import com.yougile.alertbot.rag.loader.TemplateLoader
import jakarta.annotation.PostConstruct
import org.springframework.ai.document.Document
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.SimpleVectorStore
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Service

@Service
class KnowledgeBaseService(
    private val embeddingModel: EmbeddingModel,
    private val templateLoader: TemplateLoader
) {
    private val vectorStore: VectorStore = SimpleVectorStore.builder(embeddingModel).build()
    private var initialized = false

    @PostConstruct
    fun initialize() {
        val templates = templateLoader.loadTemplates()
        if (templates.isEmpty()) {
            initialized = true
            return
        }

        val documents = templates.map { template ->
            Document(
                template.id,
                template.content,
                mapOf(
                    "type" to template.type,
                    "priority" to template.priority,
                    "category" to template.category,
                    "tags" to template.tags.joinToString(",")
                )
            )
        }

        vectorStore.add(documents)
        initialized = true
    }

    fun findSimilarTemplates(query: String, topK: Int = 3): List<TemplateDocument> {
        if (!initialized) return emptyList()

        val searchRequest = SearchRequest.builder()
            .query(query)
            .topK(topK)
            .build()

        val results = vectorStore.similaritySearch(searchRequest)

        return results.mapNotNull { doc ->
            val meta = doc.metadata
            val typeVal = (meta["type"] as? String) ?: ""
            val priorityVal = (meta["priority"] as? String) ?: ""
            val categoryVal = (meta["category"] as? String) ?: ""
            val tagsStr = (meta["tags"] as? String) ?: ""
            TemplateDocument(
                id = doc.id ?: "",
                type = typeVal,
                priority = priorityVal,
                category = categoryVal,
                tags = tagsStr.split(",").filter { it.isNotEmpty() },
                content = doc.text ?: ""
            )
        }
    }
}
