package com.yougile.alertbot.service

import com.yougile.alertbot.model.Alert
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class AlertProcessingService(
    private val openCodeService: OpenCodeService,
    private val chatModel: org.springframework.ai.chat.model.ChatModel,
    @Value("\${yougile.default.column-id}")
    private val defaultColumnId: String
) {
    fun processAlert(alert: Alert): String {
        val analysis = openCodeService.analyzeAlert(alert)

        val promptText = """
            Create a YouGile task with the following details:
            
            Title: __TITLE__
            Description: __DESCRIPTION__
            Column ID: __COLUMN_ID__
            
            Use the create_yougile_task tool to create this task.
        """.trimIndent()
            .replace("__TITLE__", analysis.title)
            .replace("__DESCRIPTION__", analysis.description)
            .replace("__COLUMN_ID__", defaultColumnId)

        val message = UserMessage(promptText)

        val mcpResponse = chatModel.call(message)

        return """
            *Task Created Successfully*
            Title: ${analysis.title}
            Category: ${analysis.category}
            Priority: ${analysis.priority}
            
            ${mcpResponse}
        """.trimIndent()
    }
}
