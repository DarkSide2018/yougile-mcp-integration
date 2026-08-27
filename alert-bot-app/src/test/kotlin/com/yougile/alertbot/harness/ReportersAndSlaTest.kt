package com.yougile.alertbot.harness

import com.yougile.alertbot.harness.dataset.GoldenAlertDataset
import com.yougile.alertbot.harness.engine.MockAgentExecutionEngine
import com.yougile.alertbot.harness.report.AgentHarnessConsoleReporter
import com.yougile.alertbot.harness.report.AgentHarnessHtmlReporter
import com.yougile.alertbot.harness.report.AgentHarnessJsonReporter
import com.yougile.alertbot.harness.runner.AgentHarnessRunner
import com.yougile.alertbot.harness.runner.AgentHarnessSla
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

class ReportersAndSlaTest {

    @Test
    @DisplayName("Reporters generate valid JSON and HTML artifacts")
    fun testReportersOutput() {
        val tempDir = Files.createTempDirectory("agent-harness-reports").toFile()
        try {
            val runner = AgentHarnessRunner(
                engine = MockAgentExecutionEngine(),
                reporters = listOf(
                    AgentHarnessConsoleReporter(),
                    AgentHarnessJsonReporter(tempDir),
                    AgentHarnessHtmlReporter(tempDir)
                )
            )

            val summary = runner.runBenchmark(GoldenAlertDataset.allScenarios)
            assertTrue(summary.passedScenarios > 0)

            val jsonFile = File(tempDir, "eval-report.json")
            val htmlFile = File(tempDir, "index.html")

            assertTrue(jsonFile.exists(), "eval-report.json must be generated")
            assertTrue(jsonFile.length() > 100, "JSON report should not be empty")

            assertTrue(htmlFile.exists(), "index.html must be generated")
            val htmlContent = htmlFile.readText()
            assertTrue(htmlContent.contains("AI Agent Evaluation Harness"), "HTML report must contain dashboard title")
            assertTrue(htmlContent.contains("SCENARIO-001"), "HTML report must contain scenario IDs")
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    @DisplayName("SLA verifier detects violations when metrics drop below thresholds")
    fun testSlaViolations() {
        val runner = AgentHarnessRunner(
            engine = MockAgentExecutionEngine(),
            reporters = emptyList(),
            sla = AgentHarnessSla(
                minPassRatePercent = 100.1 // Impossible SLA threshold to trigger violation
            )
        )

        val summary = runner.runBenchmark(GoldenAlertDataset.allScenarios)
        val violations = runner.verifySla(summary)
        assertFalse(violations.isEmpty(), "Violations should be detected when minPassRatePercent is 100.1%")
    }
}
