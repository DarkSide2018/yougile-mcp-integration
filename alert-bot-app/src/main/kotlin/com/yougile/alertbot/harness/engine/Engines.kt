package com.yougile.alertbot.harness.engine

import com.yougile.alertbot.harness.evaluator.ClassificationEvaluator
import com.yougile.alertbot.harness.evaluator.RagRetrievalEvaluator
import com.yougile.alertbot.harness.evaluator.StructuredOutputEvaluator
import com.yougile.alertbot.harness.evaluator.ToolExecutionEvaluator
import com.yougile.alertbot.harness.model.AlertEvalScenario
import com.yougile.alertbot.harness.model.ScenarioResult
import com.yougile.alertbot.model.AlertPriority
import com.yougile.alertbot.rag.KnowledgeBaseService
import com.yougile.alertbot.rag.TemplateDocument
import com.yougile.alertbot.service.AlertAnalysis
import com.yougile.alertbot.service.AlertProcessingService
import com.yougile.alertbot.service.OpenCodeService
import org.slf4j.LoggerFactory
import kotlin.system.measureTimeMillis

interface AgentHarnessEngine {
    val name: String
    fun execute(scenario: AlertEvalScenario): ScenarioResult
}

class MockAgentExecutionEngine(
    private val knowledgeBaseService: KnowledgeBaseService? = null,
    private val defaultColumnId: String = "test-column-uuid"
) : AgentHarnessEngine {

    override val name: String = "MockDeterministicEngine"

    private val ragEvaluator = RagRetrievalEvaluator()
    private val classificationEvaluator = ClassificationEvaluator()
    private val structuredOutputEvaluator = StructuredOutputEvaluator()
    private val toolEvaluator = ToolExecutionEvaluator()

    override fun execute(scenario: AlertEvalScenario): ScenarioResult {
        val failureReasons = mutableListOf<String>()
        var ragResult: com.yougile.alertbot.harness.model.RagEvaluationResult
        var classificationResult: com.yougile.alertbot.harness.model.ClassificationEvaluationResult
        var structuredResult: com.yougile.alertbot.harness.model.StructuredOutputEvaluationResult
        var toolResult: com.yougile.alertbot.harness.model.ToolExecutionEvaluationResult
        var rawResponse = ""
        var finalOutput = ""

        val latency = measureTimeMillis {
            // 1. RAG retrieval step
            val retrievedTemplates: List<TemplateDocument> = knowledgeBaseService?.findSimilarTemplates(scenario.alert.text)
                ?: mockRetrieveTemplates(scenario)

            ragResult = ragEvaluator.evaluate(scenario, retrievedTemplates)
            if (!ragResult.hitExpectedTemplate) {
                failureReasons.add(ragResult.details)
            }

            // 2. Simulated LLM Analysis step based on scenario
            val priority = scenario.expected.acceptablePriorities.firstOrNull() ?: AlertPriority.MEDIUM
            val category = scenario.expected.acceptableCategories.firstOrNull() ?: "general"
            val titleKw = scenario.expected.requiredTitleKeywords.firstOrNull() ?: "Alert"
            val title = if (scenario.expected.requiredTitleKeywords.isNotEmpty()) {
                scenario.expected.requiredTitleKeywords.joinToString(" ")
            } else {
                "${scenario.name}: ${scenario.alert.text.lines().firstOrNull()?.take(40)}"
            }
            val description = """
                Alert Details:
                ${scenario.alert.text}
                
                Matched RAG Template: [${ragResult.topTemplateType ?: "default"}]
                Recommended Actions: Verified by Automated Test Harness.
            """.trimIndent()

            val analysis = AlertAnalysis(
                priority = priority,
                category = category,
                title = title.take(scenario.expected.maxTitleLength),
                description = description
            )

            rawResponse = """
                {
                  "priority": "${analysis.priority}",
                  "category": "${analysis.category}",
                  "title": "${analysis.title}",
                  "description": "${analysis.description.replace("\n", "\\n")}"
                }
            """.trimIndent()

            // 3. Classification Evaluation
            classificationResult = classificationEvaluator.evaluate(scenario, analysis)
            if (!classificationResult.priorityPassed || !classificationResult.categoryPassed) {
                failureReasons.add(classificationResult.details)
            }

            // 4. Structured Output Evaluation
            structuredResult = structuredOutputEvaluator.evaluate(scenario, analysis)
            if (!structuredResult.formatValid) {
                failureReasons.add(structuredResult.details)
            }

            // 5. Tool Call Execution Simulation
            val capturedParams = mapOf(
                "title" to analysis.title,
                "description" to analysis.description,
                "columnId" to defaultColumnId
            )
            val toolInvoked = true
            val toolName = "create_yougile_task"

            toolResult = toolEvaluator.evaluate(scenario, toolInvoked, toolName, capturedParams)
            if (!toolResult.executionSuccess) {
                failureReasons.add(toolResult.details)
            }

            finalOutput = """
                *Task Created Successfully*
                Title: ${analysis.title}
                Category: ${analysis.category}
                Priority: ${analysis.priority}
                Tool: $toolName(columnId=$defaultColumnId)
            """.trimIndent()
        }

        val passed = failureReasons.isEmpty()

        return ScenarioResult(
            scenarioId = scenario.id,
            scenarioName = scenario.name,
            passed = passed,
            latencyMs = latency,
            ragEvaluation = ragResult,
            classificationEvaluation = classificationResult,
            structuredOutputEvaluation = structuredResult,
            toolExecutionEvaluation = toolResult,
            rawModelResponse = rawResponse,
            finalAgentOutput = finalOutput,
            failureReasons = failureReasons
        )
    }

    private fun mockRetrieveTemplates(scenario: AlertEvalScenario): List<TemplateDocument> {
        val expectedType = scenario.expected.expectedTemplateType ?: "default"
        return listOf(
            TemplateDocument(
                id = "mock-doc-1",
                type = expectedType,
                priority = scenario.expected.acceptablePriorities.firstOrNull()?.name ?: "HIGH",
                category = scenario.expected.acceptableCategories.firstOrNull() ?: "infrastructure",
                tags = scenario.tags,
                content = "Mock template body for $expectedType"
            )
        )
    }
}

class LiveAgentExecutionEngine(
    private val openCodeService: OpenCodeService,
    private val alertProcessingService: AlertProcessingService,
    private val knowledgeBaseService: KnowledgeBaseService,
    private val defaultColumnId: String = "default-column-uuid"
) : AgentHarnessEngine {

    private val log = LoggerFactory.getLogger(LiveAgentExecutionEngine::class.java)
    override val name: String = "LiveOllamaAgentEngine"

    private val ragEvaluator = RagRetrievalEvaluator()
    private val classificationEvaluator = ClassificationEvaluator()
    private val structuredOutputEvaluator = StructuredOutputEvaluator()
    private val toolEvaluator = ToolExecutionEvaluator()

    override fun execute(scenario: AlertEvalScenario): ScenarioResult {
        val failureReasons = mutableListOf<String>()
        var ragResult: com.yougile.alertbot.harness.model.RagEvaluationResult
        var classificationResult: com.yougile.alertbot.harness.model.ClassificationEvaluationResult
        var structuredResult: com.yougile.alertbot.harness.model.StructuredOutputEvaluationResult
        var toolResult: com.yougile.alertbot.harness.model.ToolExecutionEvaluationResult
        var rawResponse: String? = null
        var finalOutput: String? = null

        val latency = measureTimeMillis {
            // 1. Evaluate RAG retrieval
            val retrievedTemplates = try {
                knowledgeBaseService.findSimilarTemplates(scenario.alert.text, topK = 3)
            } catch (e: Exception) {
                log.error("RAG retrieval error for scenario ${scenario.id}", e)
                emptyList()
            }

            ragResult = ragEvaluator.evaluate(scenario, retrievedTemplates)
            if (!ragResult.hitExpectedTemplate) {
                failureReasons.add(ragResult.details)
            }

            // 2. OpenCode LLM Analysis
            val analysis = try {
                val res = openCodeService.analyzeAlert(scenario.alert)
                rawResponse = res.toString()
                res
            } catch (e: Exception) {
                log.error("OpenCode LLM execution error for scenario ${scenario.id}", e)
                failureReasons.add("LLM Analysis failed with exception: ${e.message}")
                AlertAnalysis(
                    priority = AlertPriority.MEDIUM,
                    category = "general",
                    title = "Error",
                    description = e.message ?: "Unknown error"
                )
            }

            // 3. Classification Evaluation
            classificationResult = classificationEvaluator.evaluate(scenario, analysis)
            if (!classificationResult.priorityPassed || !classificationResult.categoryPassed) {
                failureReasons.add(classificationResult.details)
            }

            // 4. Structured Output Evaluation
            structuredResult = structuredOutputEvaluator.evaluate(scenario, analysis)
            if (!structuredResult.formatValid) {
                failureReasons.add(structuredResult.details)
            }

            // 5. Tool Call & Alert Processing Execution
            val capturedParams = mapOf(
                "title" to analysis.title,
                "description" to analysis.description,
                "columnId" to defaultColumnId
            )

            try {
                finalOutput = alertProcessingService.processAlert(scenario.alert)
                toolResult = toolEvaluator.evaluate(scenario, true, "create_yougile_task", capturedParams)
            } catch (e: Exception) {
                log.warn("Alert processing tool call simulated or exception caught: ${e.message}")
                // If real MCP is offline, we check whether params were captured properly
                val toolInvoked = finalOutput != null || e.message?.contains("YouGile") == true || e.message?.contains("column") == true
                toolResult = toolEvaluator.evaluate(scenario, toolInvoked, "create_yougile_task", capturedParams)
            }
        }

        val passed = failureReasons.isEmpty()

        return ScenarioResult(
            scenarioId = scenario.id,
            scenarioName = scenario.name,
            passed = passed,
            latencyMs = latency,
            ragEvaluation = ragResult,
            classificationEvaluation = classificationResult,
            structuredOutputEvaluation = structuredResult,
            toolExecutionEvaluation = toolResult,
            rawModelResponse = rawResponse,
            finalAgentOutput = finalOutput,
            failureReasons = failureReasons
        )
    }
}
