package com.yougile.alertbot.harness

import com.yougile.alertbot.harness.evaluator.ClassificationEvaluator
import com.yougile.alertbot.harness.evaluator.RagRetrievalEvaluator
import com.yougile.alertbot.harness.evaluator.StructuredOutputEvaluator
import com.yougile.alertbot.harness.evaluator.ToolExecutionEvaluator
import com.yougile.alertbot.harness.model.AlertEvalScenario
import com.yougile.alertbot.harness.model.ExpectedOutcome
import com.yougile.alertbot.model.Alert
import com.yougile.alertbot.model.AlertPriority
import com.yougile.alertbot.rag.TemplateDocument
import com.yougile.alertbot.service.AlertAnalysis
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class EvaluatorUnitTests {

    private val sampleScenario = AlertEvalScenario(
        id = "TEST-001",
        name = "Test Scenario",
        description = "Test description",
        alert = Alert(text = "High CPU load on host-01"),
        expected = ExpectedOutcome(
            acceptablePriorities = setOf(AlertPriority.HIGH, AlertPriority.CRITICAL),
            acceptableCategories = setOf("infrastructure"),
            expectedTemplateType = "cpu",
            requiredTitleKeywords = listOf("CPU", "host-01"),
            requiredDescriptionKeywords = listOf("High CPU")
        )
    )

    @Test
    @DisplayName("RagRetrievalEvaluator correctly identifies matches and misses")
    fun testRagEvaluator() {
        val evaluator = RagRetrievalEvaluator()

        val hitTemplates = listOf(
            TemplateDocument("1", "cpu", "critical", "infrastructure", listOf("cpu"), "CPU body"),
            TemplateDocument("2", "default", "medium", "general", listOf("alert"), "Default body")
        )
        val hitResult = evaluator.evaluate(sampleScenario, hitTemplates)
        assertTrue(hitResult.hitExpectedTemplate)

        val missTemplates = listOf(
            TemplateDocument("3", "memory", "high", "infrastructure", listOf("memory"), "Memory body")
        )
        val missResult = evaluator.evaluate(sampleScenario, missTemplates)
        assertFalse(missResult.hitExpectedTemplate)
    }

    @Test
    @DisplayName("ClassificationEvaluator checks priority and category accurately")
    fun testClassificationEvaluator() {
        val evaluator = ClassificationEvaluator()

        val validAnalysis = AlertAnalysis(
            priority = AlertPriority.HIGH,
            category = "infrastructure",
            title = "High CPU on host-01",
            description = "High CPU load on host-01 details"
        )
        val validResult = evaluator.evaluate(sampleScenario, validAnalysis)
        assertTrue(validResult.priorityPassed)
        assertTrue(validResult.categoryPassed)

        val invalidAnalysis = AlertAnalysis(
            priority = AlertPriority.LOW,
            category = "marketing",
            title = "Test",
            description = "Test"
        )
        val invalidResult = evaluator.evaluate(sampleScenario, invalidAnalysis)
        assertFalse(invalidResult.priorityPassed)
        assertFalse(invalidResult.categoryPassed)
    }

    @Test
    @DisplayName("StructuredOutputEvaluator validates length and keyword presence")
    fun testStructuredOutputEvaluator() {
        val evaluator = StructuredOutputEvaluator()

        val validAnalysis = AlertAnalysis(
            priority = AlertPriority.HIGH,
            category = "infrastructure",
            title = "CPU load issue on host-01",
            description = "High CPU load on host-01 exceeded threshold by 20% for 10 minutes"
        )
        val validResult = evaluator.evaluate(sampleScenario, validAnalysis)
        assertTrue(validResult.formatValid)
        assertTrue(validResult.titleKeywordsMissing.isEmpty())
        assertTrue(validResult.descriptionKeywordsMissing.isEmpty())

        val missingKwAnalysis = AlertAnalysis(
            priority = AlertPriority.HIGH,
            category = "infrastructure",
            title = "Memory issue on server",
            description = "Something went wrong with server memory"
        )
        val missingKwResult = evaluator.evaluate(sampleScenario, missingKwAnalysis)
        assertFalse(missingKwResult.formatValid)
        assertFalse(missingKwResult.titleKeywordsMissing.isEmpty())
    }

    @Test
    @DisplayName("ToolExecutionEvaluator validates MCP tool call params")
    fun testToolEvaluator() {
        val evaluator = ToolExecutionEvaluator()

        val validParams = mapOf(
            "title" to "CPU issue",
            "description" to "Long enough description of CPU issue",
            "columnId" to "col-123"
        )
        val validResult = evaluator.evaluate(sampleScenario, true, "create_yougile_task", validParams)
        assertTrue(validResult.executionSuccess)

        val missingParams = mapOf(
            "title" to "CPU issue"
        )
        val invalidResult = evaluator.evaluate(sampleScenario, true, "create_yougile_task", missingParams)
        assertFalse(invalidResult.executionSuccess)

        val wrongToolResult = evaluator.evaluate(sampleScenario, true, "delete_everything", validParams)
        assertFalse(wrongToolResult.executionSuccess)
    }
}
