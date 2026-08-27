package com.yougile.alertbot.harness

import com.yougile.alertbot.harness.dataset.GoldenAlertDataset
import com.yougile.alertbot.harness.engine.MockAgentExecutionEngine
import com.yougile.alertbot.harness.runner.AgentHarnessRunner
import com.yougile.alertbot.harness.runner.AgentHarnessSla
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class AgentHarnessBenchmarkTest {

    private val runner = AgentHarnessRunner(
        engine = MockAgentExecutionEngine(),
        sla = AgentHarnessSla(
            minPassRatePercent = 85.0,
            minRagHitRatePercent = 80.0,
            minPriorityAccuracyPercent = 80.0,
            minCategoryAccuracyPercent = 80.0,
            minToolInvocationRatePercent = 80.0
        )
    )

    @Test
    @DisplayName("Run complete Golden Benchmark Suite and enforce SLA metrics")
    fun testGoldenBenchmarkSuite() {
        val summary = runner.runBenchmark(GoldenAlertDataset.allScenarios)

        println("Evaluated ${summary.totalScenarios} scenarios, Pass Rate: ${summary.passRatePercent}%")

        val violations = runner.verifySla(summary)
        assertTrue(violations.isEmpty(), "SLA violations detected: $violations")
        assertTrue(summary.passedScenarios >= 7, "Expected at least 7 passing scenarios")
    }

    @Test
    @DisplayName("Verify individual scenario: CPU Critical Alert")
    fun testCpuScenario() {
        val scenario = GoldenAlertDataset.getScenario("SCENARIO-001-CPU-CRITICAL")!!
        val engine = MockAgentExecutionEngine()
        val result = engine.execute(scenario)

        assertTrue(result.passed, "Scenario should pass: ${result.failureReasons}")
        assertTrue(result.ragEvaluation.hitExpectedTemplate)
        assertTrue(result.classificationEvaluation.priorityPassed)
        assertTrue(result.toolExecutionEvaluation.executionSuccess)
    }

    @Test
    @DisplayName("Verify individual scenario: JVM Memory Leak")
    fun testMemoryLeakScenario() {
        val scenario = GoldenAlertDataset.getScenario("SCENARIO-002-MEM-LEAK-HIGH")!!
        val engine = MockAgentExecutionEngine()
        val result = engine.execute(scenario)

        assertTrue(result.passed, "Scenario should pass: ${result.failureReasons}")
        assertTrue(result.ragEvaluation.hitExpectedTemplate)
        assertTrue(result.classificationEvaluation.priorityPassed)
    }
}
