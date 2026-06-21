package com.yougile.alertbot.service

import com.yougile.alertbot.model.Alert
import org.springframework.ai.chat.client.ChatClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class AlertProcessingService(
    private val openCodeService: OpenCodeService,
    private val chatClientBuilder: ChatClient.Builder,
    @Value("\${yougile.default.column-id}")
    private val defaultColumnId: String
) {
    fun processAlert(alert: Alert): String {
        val analysis = openCodeService.analyzeAlert(alert)

        val response = chatClientBuilder.build().prompt()
            .user { it.text("""
                Create a YouGile task with the following details:
                
                Title: ${analysis.title}
                Description: ${analysis.description}
                Column ID: $defaultColumnId
                
                Use the create_yougile_task tool to create this task.
            """.trimIndent()) }
            .tools()
            .call()
            .content()

        return """
            *Task Created Successfully*
            Title: ${analysis.title}
            Category: ${analysis.category}
            Priority: ${analysis.priority}
            
            $response
        """.trimIndent()
    }
}
