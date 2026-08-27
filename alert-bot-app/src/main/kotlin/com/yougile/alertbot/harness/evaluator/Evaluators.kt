package com.yougile.alertbot.harness.evaluator

import com.yougile.alertbot.harness.model.AlertEvalScenario
import com.yougile.alertbot.harness.model.ClassificationEvaluationResult
import com.yougile.alertbot.harness.model.RagEvaluationResult
import com.yougile.alertbot.harness.model.StructuredOutputEvaluationResult
import com.yougile.alertbot.harness.model.ToolExecutionEvaluationResult
import com.yougile.alertbot.model.AlertPriority
import com.yougile.alertbot.rag.TemplateDocument
import com.yougile.alertbot.service.AlertAnalysis

class RagRetrievalEvaluator {

    fun evaluate(scenario: AlertEvalScenario, retrievedTemplates: List<TemplateDocument>): RagEvaluationResult {
        val expectedType = scenario.expected.expectedTemplateType
        val retrievedTypes = retrievedTemplates.map { it.type }
        val topType = retrievedTypes.firstOrNull()

        val hitExpected = if (expectedType == null || expectedType.equals("default", ignoreCase = true)) {
            // Default template or no strict expectation is considered a pass
            true
        } else {
            retrievedTypes.any { it.equals(expectedType, ignoreCase = true) }
        }

        val details = if (hitExpected) {
            "RAG retrieved ${retrievedTemplates.size} templates. Top: [$topType], Expected: [$expectedType]."
        } else {
            "RAG failed to retrieve expected template [$expectedType]. Retrieved types: $retrievedTypes."
        }

        return RagEvaluationResult(
            retrievedTemplatesCount = retrievedTemplates.size,
            topTemplateType = topType,
            hitExpectedTemplate = hitExpected,
            retrievedTemplateTypes = retrievedTypes,
            details = details
        )
    }
}

class ClassificationEvaluator {

    fun evaluate(scenario: AlertEvalScenario, analysis: AlertAnalysis): ClassificationEvaluationResult {
        val priorityPassed = scenario.expected.acceptablePriorities.contains(analysis.priority)
        val actualCatLower = analysis.category.lowercase().trim()
        val categoryPassed = scenario.expected.acceptableCategories.isEmpty() ||
                scenario.expected.acceptableCategories.any { acceptable ->
                    actualCatLower.contains(acceptable) || acceptable.contains(actualCatLower)
                }

        val details = buildString {
            if (!priorityPassed) {
                append("Priority mismatch: got [${analysis.priority}], expected one of ${scenario.expected.acceptablePriorities}. ")
            }
            if (!categoryPassed) {
                append("Category mismatch: got [${analysis.category}], expected one of ${scenario.expected.acceptableCategories}.")
            }
            if (priorityPassed && categoryPassed) {
                append("Classification passed (Priority: ${analysis.priority}, Category: ${analysis.category}).")
            }
        }

        return ClassificationEvaluationResult(
            actualPriority = analysis.priority,
            priorityPassed = priorityPassed,
            actualCategory = analysis.category,
            categoryPassed = categoryPassed,
            details = details
        )
    }
}

class StructuredOutputEvaluator {

    fun evaluate(scenario: AlertEvalScenario, analysis: AlertAnalysis): StructuredOutputEvaluationResult {
        val title = analysis.title.trim()
        val description = analysis.description.trim()

        val titleLengthValid = title.isNotEmpty() && title.length <= scenario.expected.maxTitleLength
        val descLengthValid = description.length >= scenario.expected.minDescriptionLength

        val titleKeywordsMatched = mutableListOf<String>()
        val titleKeywordsMissing = mutableListOf<String>()
        for (kw in scenario.expected.requiredTitleKeywords) {
            if (title.contains(kw, ignoreCase = true) || description.contains(kw, ignoreCase = true)) {
                titleKeywordsMatched.add(kw)
            } else {
                titleKeywordsMissing.add(kw)
            }
        }

        val descKeywordsMatched = mutableListOf<String>()
        val descKeywordsMissing = mutableListOf<String>()
        for (kw in scenario.expected.requiredDescriptionKeywords) {
            if (description.contains(kw, ignoreCase = true) || title.contains(kw, ignoreCase = true)) {
                descKeywordsMatched.add(kw)
            } else {
                descKeywordsMissing.add(kw)
            }
        }

        val formatValid = titleLengthValid && descLengthValid

        val details = buildString {
            if (!titleLengthValid) append("Title invalid length (${title.length} chars). ")
            if (!descLengthValid) append("Description too short (${description.length} chars). ")
            if (titleKeywordsMissing.isNotEmpty()) append("Missing title keywords: $titleKeywordsMissing. ")
            if (descKeywordsMissing.isNotEmpty()) append("Missing description keywords: $descKeywordsMissing. ")
            if (formatValid && titleKeywordsMissing.isEmpty() && descKeywordsMissing.isEmpty()) {
                append("Structured output format and content keywords fully validated.")
            }
        }

        return StructuredOutputEvaluationResult(
            actualTitle = title,
            titleLengthValid = titleLengthValid,
            titleKeywordsMatched = titleKeywordsMatched,
            titleKeywordsMissing = titleKeywordsMissing,
            actualDescription = description,
            descriptionLengthValid = descLengthValid,
            descriptionKeywordsMatched = descKeywordsMatched,
            descriptionKeywordsMissing = descKeywordsMissing,
            formatValid = formatValid && titleKeywordsMissing.isEmpty() && descKeywordsMissing.isEmpty(),
            details = details
        )
    }
}

class ToolExecutionEvaluator {

    fun evaluate(
        scenario: AlertEvalScenario,
        toolInvoked: Boolean,
        toolName: String?,
        capturedParams: Map<String, Any?>
    ): ToolExecutionEvaluationResult {
        val expectedTool = scenario.expected.expectedToolName
        val toolMatches = toolInvoked && toolName == expectedTool
        val missingParams = scenario.expected.requiredToolParams.filter { !capturedParams.containsKey(it) || capturedParams[it] == null }
        val paramsValid = toolMatches && missingParams.isEmpty()

        val details = if (paramsValid) {
            "Tool [$toolName] invoked with all required parameters: ${capturedParams.keys}."
        } else if (!toolInvoked) {
            "Tool invocation was not recorded."
        } else if (!toolMatches) {
            "Unexpected tool invoked: [$toolName], expected: [$expectedTool]."
        } else {
            "Tool parameters missing: $missingParams."
        }

        return ToolExecutionEvaluationResult(
            toolInvoked = toolInvoked,
            toolName = toolName,
            capturedParams = capturedParams,
            paramsValid = paramsValid,
            executionSuccess = paramsValid,
            details = details
        )
    }
}
