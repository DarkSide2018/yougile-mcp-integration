package com.yougile.alertbot.harness.model

import com.yougile.alertbot.model.Alert

/**
 * Encapsulates a benchmark test scenario for evaluating agent accuracy, RAG, and MCP tool usage.
 */
data class AlertEvalScenario(
    val id: String,
    val name: String,
    val description: String,
    val alert: Alert,
    val expected: ExpectedOutcome,
    val tags: List<String> = emptyList()
)
