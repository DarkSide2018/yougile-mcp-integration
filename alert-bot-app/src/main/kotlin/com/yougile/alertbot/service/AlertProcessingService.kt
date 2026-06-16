package com.yougile.alertbot.service

import com.yougile.alertbot.model.Alert
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class AlertProcessingService(
    private val openCodeService: OpenCodeService,
    private val chatModel: ChatModel,
    @Value("\${yougile.default.column-id}") private val defaultColumnId: String
) {
    fun processAlert(alert: Alert): String {
        val analysis = openCodeService.analyzeAlert(alert)

        val mcpPrompt = PromptTemplate("""
            Create a YouGile task with the following details:
            
            Title: {title}
            Description: {description}
            Column ID: {columnId}
            
            Use the create_yougile_task tool to create this task.
        """.trimIndent())

        val message = mcpPrompt.createMessage(mapOf(
            "title" to analysis.title,
            "description" to analysis.description,
            "columnId" to defaultColumnId
        ))

        val mcpResponse = chatModel.call(message)

        return """
            *Task Created Successfully*
            Title: ${analysis.title}
            Category: ${analysis.category}
            Priority: ${analysis.priority}
            
            ${mcpResponse.toString()}
        """.trimIndent()
    }
}
