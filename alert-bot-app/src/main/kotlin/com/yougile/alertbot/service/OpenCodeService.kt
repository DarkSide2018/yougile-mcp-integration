package com.yougile.alertbot.service

import com.yougile.alertbot.model.Alert
import com.yougile.alertbot.model.AlertPriority
import com.yougile.alertbot.rag.KnowledgeBaseService
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.ollama.OllamaChatModel
import org.springframework.stereotype.Service
import java.util.regex.Pattern

@Service
class OpenCodeService(
    private val chatModel: OllamaChatModel,
    private val knowledgeBaseService: KnowledgeBaseService
) {
    fun analyzeAlert(alert: Alert): AlertAnalysis {
        val similarTemplates = knowledgeBaseService.findSimilarTemplates(alert.text)
        val templateContext = if (similarTemplates.isNotEmpty()) {
            "Similar templates found:\n${similarTemplates.joinToString("\n---\n") { it.content }}"
        } else {
            "No similar templates found."
        }

        val promptText = """
            Analyze this alert and determine:
            - priority (CRITICAL, HIGH, MEDIUM, LOW)
            - category (infrastructure, security, application, database, network)
            - suggested title for the task (brief, max 80 chars)
            - suggested description (detailed)
            
            Alert text:
            __ALERT__
            
            __TEMPLATES__
            
            Respond in JSON format:
            {
              "priority": "...",
              "category": "...",
              "title": "...",
              "description": "..."
            }
        """.trimIndent()
            .replace("__ALERT__", alert.text)
            .replace("__TEMPLATES__", templateContext)

        val message = UserMessage(promptText)

        val response = chatModel.call(message)
        val content = response.toString()
        return parseResponse(content)
    }

    private fun parseResponse(response: String): AlertAnalysis {
        val jsonMatch = Pattern.compile("\\{.*}").matcher(response.replace("\n", " "))
        return if (jsonMatch.find()) {
            val json = jsonMatch.group()
            try {
                val mapper = com.fasterxml.jackson.databind.ObjectMapper()
                val node = mapper.readTree(json)
                AlertAnalysis(
                    priority = AlertPriority.valueOf(
                        node.get("priority")?.asText()?.uppercase() ?: "MEDIUM"
                    ),
                    category = node.get("category")?.asText() ?: "general",
                    title = node.get("title")?.asText() ?: "Alert: ${response.take(50)}",
                    description = node.get("description")?.asText() ?: response
                )
            } catch (e: Exception) {
                AlertAnalysis(
                    priority = AlertPriority.MEDIUM,
                    category = "general",
                    title = "Alert: ${response.take(50)}",
                    description = response
                )
            }
        } else {
            AlertAnalysis(
                priority = AlertPriority.MEDIUM,
                category = "general",
                title = "Alert",
                description = response
            )
        }
    }
}

data class AlertAnalysis(
    val priority: AlertPriority,
    val category: String,
    val title: String,
    val description: String
)
