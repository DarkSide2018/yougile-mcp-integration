package com.yougile.alertbot.harness.model

import com.yougile.alertbot.model.AlertPriority

/**
 * Defines the expected evaluation ground truth for an alert scenario.
 */
data class ExpectedOutcome(
    val acceptablePriorities: Set<AlertPriority>,
    val acceptableCategories: Set<String>,
    val expectedTemplateType: String? = null,
    val requiredTitleKeywords: List<String> = emptyList(),
    val requiredDescriptionKeywords: List<String> = emptyList(),
    val maxTitleLength: Int = 100,
    val minDescriptionLength: Int = 20,
    val expectedToolName: String = "create_yougile_task",
    val requiredToolParams: Set<String> = setOf("title", "description", "columnId")
) {
    constructor(
        priority: AlertPriority,
        category: String,
        expectedTemplateType: String? = null,
        requiredTitleKeywords: List<String> = emptyList(),
        requiredDescriptionKeywords: List<String> = emptyList()
    ) : this(
        acceptablePriorities = setOf(priority),
        acceptableCategories = setOf(category.lowercase()),
        expectedTemplateType = expectedTemplateType,
        requiredTitleKeywords = requiredTitleKeywords,
        requiredDescriptionKeywords = requiredDescriptionKeywords
    )
}
