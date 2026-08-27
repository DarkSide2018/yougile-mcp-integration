package com.yougile.alertbot.harness.runner

import com.yougile.alertbot.harness.dataset.GoldenAlertDataset
import com.yougile.alertbot.harness.engine.AgentHarnessEngine
import com.yougile.alertbot.harness.engine.MockAgentExecutionEngine
import com.yougile.alertbot.harness.model.AgentEvaluationSummary
import com.yougile.alertbot.harness.model.AlertEvalScenario
import com.yougile.alertbot.harness.model.ScenarioResult
import com.yougile.alertbot.harness.report.AgentHarnessConsoleReporter
import com.yougile.alertbot.harness.report.AgentHarnessHtmlReporter
import com.yougile.alertbot.harness.report.AgentHarnessJsonReporter
import com.yougile.alertbot.harness.report.AgentHarnessReporter
import org.slf4j.LoggerFactory

data class AgentHarnessSla(
    val minPassRatePercent: Double = 80.0,
    val minRagHitRatePercent: Double = 75.0,
    val minPriorityAccuracyPercent: Double = 75.0,
    val minCategoryAccuracyPercent: Double = 75.0,
    val minToolInvocationRatePercent: Double = 80.0,
    val maxP95LatencyMs: Long = 10000
)

class AgentHarnessRunner(
    private val engine: AgentHarnessEngine = MockAgentExecutionEngine(),
    private val reporters: List<AgentHarnessReporter> = listOf(
        AgentHarnessConsoleReporter(),
        AgentHarnessJsonReporter(),
        AgentHarnessHtmlReporter()
    ),
    private val sla: AgentHarnessSla = AgentHarnessSla()
) {

    private val log = LoggerFactory.getLogger(AgentHarnessRunner::class.java)

    fun runBenchmark(
        scenarios: List<AlertEvalScenario> = GoldenAlertDataset.allScenarios
    ): AgentEvaluationSummary {
        log.info("Starting Agent Evaluation Benchmark using engine [${engine.name}] with ${scenarios.size} scenarios...")

        val results = mutableListOf<ScenarioResult>()

        for (scenario in scenarios) {
            log.info("Executing evaluation scenario [${scenario.id}]: ${scenario.name}")
            val res = engine.execute(scenario)
            results.add(res)
        }

        val total = results.size
        val passed = results.count { it.passed }
        val failed = total - passed
        val passRate = if (total > 0) (passed.toDouble() / total) * 100.0 else 0.0

        val ragHits = results.count { it.ragEvaluation.hitExpectedTemplate }
        val ragHitRate = if (total > 0) (ragHits.toDouble() / total) * 100.0 else 0.0

        val prioMatches = results.count { it.classificationEvaluation.priorityPassed }
        val prioAcc = if (total > 0) (prioMatches.toDouble() / total) * 100.0 else 0.0

        val catMatches = results.count { it.classificationEvaluation.categoryPassed }
        val catAcc = if (total > 0) (catMatches.toDouble() / total) * 100.0 else 0.0

        val toolSuccesses = results.count { it.toolExecutionEvaluation.executionSuccess }
        val toolRate = if (total > 0) (toolSuccesses.toDouble() / total) * 100.0 else 0.0

        val latencies = results.map { it.latencyMs }.sorted()
        val avgLatency = if (latencies.isNotEmpty()) latencies.average() else 0.0
        val p50 = if (latencies.isNotEmpty()) latencies[(latencies.size * 0.50).toInt().coerceAtMost(latencies.size - 1)] else 0L
        val p95 = if (latencies.isNotEmpty()) latencies[(latencies.size * 0.95).toInt().coerceAtMost(latencies.size - 1)] else 0L
        val p99 = if (latencies.isNotEmpty()) latencies[(latencies.size * 0.99).toInt().coerceAtMost(latencies.size - 1)] else 0L
        val minLat = latencies.firstOrNull() ?: 0L
        val maxLat = latencies.lastOrNull() ?: 0L

        val summary = AgentEvaluationSummary(
            totalScenarios = total,
            passedScenarios = passed,
            failedScenarios = failed,
            passRatePercent = passRate,
            ragHitRatePercent = ragHitRate,
            priorityAccuracyPercent = prioAcc,
            categoryAccuracyPercent = catAcc,
            toolInvocationRatePercent = toolRate,
            avgLatencyMs = avgLatency,
            p50LatencyMs = p50,
            p95LatencyMs = p95,
            p99LatencyMs = p99,
            minLatencyMs = minLat,
            maxLatencyMs = maxLat,
            results = results,
            environmentInfo = mapOf(
                "engine" to engine.name,
                "javaVersion" to System.getProperty("java.version"),
                "os" to "${System.getProperty("os.name")} ${System.getProperty("os.arch")}"
            )
        )

        for (reporter in reporters) {
            reporter.report(summary)
        }

        return summary
    }

    fun verifySla(summary: AgentEvaluationSummary): List<String> {
        val violations = mutableListOf<String>()
        if (summary.passRatePercent < sla.minPassRatePercent) {
            violations.add("SLA Breach: Pass rate %.1f%% is below minimum %.1f%%".format(summary.passRatePercent, sla.minPassRatePercent))
        }
        if (summary.ragHitRatePercent < sla.minRagHitRatePercent) {
            violations.add("SLA Breach: RAG Hit rate %.1f%% is below minimum %.1f%%".format(summary.ragHitRatePercent, sla.minRagHitRatePercent))
        }
        if (summary.priorityAccuracyPercent < sla.minPriorityAccuracyPercent) {
            violations.add("SLA Breach: Priority accuracy %.1f%% is below minimum %.1f%%".format(summary.priorityAccuracyPercent, sla.minPriorityAccuracyPercent))
        }
        if (summary.categoryAccuracyPercent < sla.minCategoryAccuracyPercent) {
            violations.add("SLA Breach: Category accuracy %.1f%% is below minimum %.1f%%".format(summary.categoryAccuracyPercent, sla.minCategoryAccuracyPercent))
        }
        if (summary.toolInvocationRatePercent < sla.minToolInvocationRatePercent) {
            violations.add("SLA Breach: Tool invocation rate %.1f%% is below minimum %.1f%%".format(summary.toolInvocationRatePercent, sla.minToolInvocationRatePercent))
        }
        if (summary.p95LatencyMs > sla.maxP95LatencyMs) {
            violations.add("SLA Breach: P95 latency ${summary.p95LatencyMs}ms exceeds max allowed ${sla.maxP95LatencyMs}ms")
        }
        return violations
    }
}
