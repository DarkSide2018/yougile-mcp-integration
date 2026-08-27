package com.yougile.alertbot.harness.model

import com.yougile.alertbot.model.AlertPriority

data class RagEvaluationResult(
    val retrievedTemplatesCount: Int,
    val topTemplateType: String?,
    val hitExpectedTemplate: Boolean,
    val retrievedTemplateTypes: List<String>,
    val details: String
)

data class ClassificationEvaluationResult(
    val actualPriority: AlertPriority,
    val priorityPassed: Boolean,
    val actualCategory: String,
    val categoryPassed: Boolean,
    val details: String
)

data class StructuredOutputEvaluationResult(
    val actualTitle: String,
    val titleLengthValid: Boolean,
    val titleKeywordsMatched: List<String>,
    val titleKeywordsMissing: List<String>,
    val actualDescription: String,
    val descriptionLengthValid: Boolean,
    val descriptionKeywordsMatched: List<String>,
    val descriptionKeywordsMissing: List<String>,
    val formatValid: Boolean,
    val details: String
)

data class ToolExecutionEvaluationResult(
    val toolInvoked: Boolean,
    val toolName: String?,
    val capturedParams: Map<String, Any?>,
    val paramsValid: Boolean,
    val executionSuccess: Boolean,
    val details: String
)

data class ScenarioResult(
    val scenarioId: String,
    val scenarioName: String,
    val passed: Boolean,
    val latencyMs: Long,
    val ragEvaluation: RagEvaluationResult,
    val classificationEvaluation: ClassificationEvaluationResult,
    val structuredOutputEvaluation: StructuredOutputEvaluationResult,
    val toolExecutionEvaluation: ToolExecutionEvaluationResult,
    val rawModelResponse: String? = null,
    val finalAgentOutput: String? = null,
    val failureReasons: List<String> = emptyList()
)
