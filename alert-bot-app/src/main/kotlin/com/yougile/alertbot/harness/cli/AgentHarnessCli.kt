package com.yougile.alertbot.harness.cli

import com.yougile.alertbot.harness.dataset.GoldenAlertDataset
import com.yougile.alertbot.harness.engine.MockAgentExecutionEngine
import com.yougile.alertbot.harness.runner.AgentHarnessRunner
import com.yougile.alertbot.harness.runner.AgentHarnessSla
import kotlin.system.exitProcess

/**
 * Command-line entrypoint for running the AI Agent Evaluation Harness.
 */
object AgentHarnessCli {

    @JvmStatic
    fun main(args: Array<String>) {
        println("=================================================================")
        println("           STARTING AI AGENT EVALUATION HARNESS CLI               ")
        println("=================================================================")

        val runner = AgentHarnessRunner(
            engine = MockAgentExecutionEngine(),
            sla = AgentHarnessSla(
                minPassRatePercent = 85.0,
                minRagHitRatePercent = 80.0
            )
        )

        val summary = runner.runBenchmark(GoldenAlertDataset.allScenarios)
        val violations = runner.verifySla(summary)

        if (violations.isNotEmpty()) {
            System.err.println("❌ AGENT HARNESS EVALUATION FAILED SLA CHECKS:")
            violations.forEach { System.err.println("  - $it") }
            exitProcess(1)
        } else {
            println("🎉 ALL AGENT HARNESS QUALITY CHECKS PASSED SUCCESSFULLY!")
            exitProcess(0)
        }
    }
}
