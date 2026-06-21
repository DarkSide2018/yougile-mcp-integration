package com.yougile.mcp.tools

import org.slf4j.LoggerFactory
import org.springaicommunity.mcp.annotation.McpTool
import org.springaicommunity.mcp.annotation.McpToolParam
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class YougileTaskTools(
    private val yougileWebClient: WebClient
) {
    private val log = LoggerFactory.getLogger(YougileTaskTools::class.java)

    @McpTool(name = "create_yougile_task", description = "Create a new task in YouGile")
    fun createYougileTask(
        @McpToolParam(description = "Task title") title: String,
        @McpToolParam(description = "Task description") description: String,
        @McpToolParam(description = "Column ID to place the task in") columnId: String
    ): String {
        log.info(">>> Tool called: create_yougile_task(title='{}', columnId={})", title, columnId)

        val body = mapOf(
            "title" to title,
            "description" to description,
            "columnId" to columnId
        )

        val response = yougileWebClient.post()
            .uri("/tasks")
            .bodyValue(body)
            .retrieve()
            .bodyToMono<Map<String, Any>>()
            .block()

        val taskId = response?.get("id") ?: response?.get("content")
        log.info("<<< create_yougile_task completed: taskId={}", taskId)
        return "Task created successfully. ID: $taskId"
    }

    @McpTool(name = "list_yougile_boards", description = "List all boards in YouGile")
    fun listYougileBoards(): String {
        log.info(">>> Tool called: list_yougile_boards")

        val response = yougileWebClient.get()
            .uri("/boards")
            .retrieve()
            .bodyToMono<Map<String, Any>>()
            .block()

        @Suppress("UNCHECKED_CAST")
        val boards = response?.get("content") as? List<Map<String, Any>> ?: emptyList()

        log.info("<<< list_yougile_boards completed: {} boards found", boards.size)
        return if (boards.isEmpty()) {
            "No boards found"
        } else {
            boards.joinToString("\n") { board ->
                val id = board["id"] ?: "unknown"
                val title = board["title"] ?: "Untitled"
                "- $title (id: $id)"
            }
        }
    }

    @McpTool(name = "list_yougile_users", description = "List all users in YouGile")
    fun listYougileUsers(): String {
        log.info(">>> Tool called: list_yougile_users")

        val response = yougileWebClient.get()
            .uri("/users")
            .retrieve()
            .bodyToMono<Map<String, Any>>()
            .block()

        @Suppress("UNCHECKED_CAST")
        val users = response?.get("content") as? List<Map<String, Any>> ?: emptyList()

        log.info("<<< list_yougile_users completed: {} users found", users.size)
        return if (users.isEmpty()) {
            "No users found"
        } else {
            users.joinToString("\n") { user ->
                val id = user["id"] ?: "unknown"
                val name = user["name"] ?: user["email"] ?: "Unknown"
                "- $name (id: $id)"
            }
        }
    }

    @McpTool(name = "list_yougile_columns", description = "List columns for a specific board")
    fun listYougileColumns(
        @McpToolParam(description = "Board ID to list columns for") boardId: String
    ): String {
        log.info(">>> Tool called: list_yougile_columns(boardId={})", boardId)

        val board = yougileWebClient.get()
            .uri("/boards/{id}", boardId)
            .retrieve()
            .bodyToMono<Map<String, Any>>()
            .block()

        @Suppress("UNCHECKED_CAST")
        val columns = board?.get("columns") as? List<Map<String, Any>> ?: emptyList()

        log.info("<<< list_yougile_columns completed: {} columns found", columns.size)
        return if (columns.isEmpty()) {
            "No columns found for board $boardId"
        } else {
            columns.joinToString("\n") { col ->
                val id = col["id"] ?: "unknown"
                val title = col["title"] ?: col["name"] ?: "Untitled"
                "- $title (id: $id)"
            }
        }
    }
}
