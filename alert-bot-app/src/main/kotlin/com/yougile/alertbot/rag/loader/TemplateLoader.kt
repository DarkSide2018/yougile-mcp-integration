package com.yougile.alertbot.rag.loader

import com.yougile.alertbot.rag.TemplateDocument
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.stereotype.Component

@Component
class TemplateLoader {

    private val resolver = PathMatchingResourcePatternResolver()

    fun loadTemplates(): List<TemplateDocument> {
        val resources = resolver.getResources("classpath:templates/*.md")
        return resources.mapNotNull { it.loadTemplate() }
    }

    private fun Resource.loadTemplate(): TemplateDocument? {
        return try {
            val content = this.inputStream.bufferedReader().readText()
            parseTemplate(content)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseTemplate(content: String): TemplateDocument {
        val frontmatter = mutableMapOf<String, String>()
        val tags = mutableListOf<String>()
        var body = content

        if (content.startsWith("---")) {
            val endIndex = content.indexOf("---", 3)
            if (endIndex > 0) {
                val yaml = content.substring(3, endIndex).trim()
                yaml.lines().forEach { line ->
                    val colonIndex = line.indexOf(":")
                    if (colonIndex > 0) {
                        val key = line.substring(0, colonIndex).trim()
                        val value = line.substring(colonIndex + 1).trim()
                        if (key == "tags") {
                            val tagContent = value.removePrefix("[").removeSuffix("]")
                            tags.addAll(tagContent.split(",").map { it.trim().removeSurrounding("\"") })
                        } else {
                            frontmatter[key] = value
                        }
                    }
                }
                body = content.substring(endIndex + 3).trim()
            }
        }

        return TemplateDocument(
            id = java.util.UUID.randomUUID().toString(),
            type = frontmatter["type"] ?: "default",
            priority = frontmatter["priority"] ?: "medium",
            category = frontmatter["category"] ?: "general",
            tags = tags,
            content = body
        )
    }
}
