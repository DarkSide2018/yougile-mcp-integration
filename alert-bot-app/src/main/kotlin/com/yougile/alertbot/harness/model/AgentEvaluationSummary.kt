package com.yougile.alertbot.harness.model

import java.time.Instant

data class AgentEvaluationSummary(
    val timestamp: Instant = Instant.now(),
    val totalScenarios: Int,
    val passedScenarios: Int,
    val failedScenarios: Int,
    val passRatePercent: Double,
    val ragHitRatePercent: Double,
    val priorityAccuracyPercent: Double,
    val categoryAccuracyPercent: Double,
    val toolInvocationRatePercent: Double,
    val avgLatencyMs: Double,
    val p50LatencyMs: Long,
    val p95LatencyMs: Long,
    val p99LatencyMs: Long,
    val minLatencyMs: Long,
    val maxLatencyMs: Long,
    val results: List<ScenarioResult>,
    val environmentInfo: Map<String, String> = emptyMap()
) {
    val isAllPassed: Boolean get() = failedScenarios == 0
}
