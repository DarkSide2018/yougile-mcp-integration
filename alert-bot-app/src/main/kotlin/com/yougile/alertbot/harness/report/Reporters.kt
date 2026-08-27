package com.yougile.alertbot.harness.report

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.yougile.alertbot.harness.model.AgentEvaluationSummary
import com.yougile.alertbot.harness.model.ScenarioResult
import java.io.File
import java.time.format.DateTimeFormatter

interface AgentHarnessReporter {
    fun report(summary: AgentEvaluationSummary)
}

class AgentHarnessConsoleReporter : AgentHarnessReporter {

    override fun report(summary: AgentEvaluationSummary) {
        val border = "=".repeat(88)
        val divider = "-".repeat(88)

        println()
        println(border)
        println("                     🚀 AI AGENT EVALUATION HARNESS REPORT                      ")
        println(border)
        println(" Timestamp: ${DateTimeFormatter.ISO_INSTANT.format(summary.timestamp)}")
        println(" Total Scenarios: ${summary.totalScenarios} | Passed: ${summary.passedScenarios} | Failed: ${summary.failedScenarios}")
        println(" Overall Pass Rate: %.1f%%".format(summary.passRatePercent))
        println(divider)
        println(" 📊 ACCURACY & QUALITY METRICS:")
        println("   - RAG Retrieval Hit Rate:        %6.1f%%".format(summary.ragHitRatePercent))
        println("   - Priority Classification Acc:   %6.1f%%".format(summary.priorityAccuracyPercent))
        println("   - Category Classification Acc:   %6.1f%%".format(summary.categoryAccuracyPercent))
        println("   - Tool Invocation Success Rate:  %6.1f%%".format(summary.toolInvocationRatePercent))
        println(divider)
        println(" ⚡ PERFORMANCE & LATENCY METRICS:")
        println("   - Avg Latency:  %6.1f ms".format(summary.avgLatencyMs))
        println("   - P50 Latency:  %6d ms".format(summary.p50LatencyMs))
        println("   - P95 Latency:  %6d ms".format(summary.p95LatencyMs))
        println("   - Min / Max:    %6d ms / %d ms".format(summary.minLatencyMs, summary.maxLatencyMs))
        println(divider)
        println(" 📋 SCENARIO BREAKDOWN:")
        println(" %-28s | %-8s | %-10s | %-10s | %-8s".format("Scenario ID", "Status", "RAG Hit", "Priority", "Latency"))
        println(divider)

        for (res in summary.results) {
            val statusBadge = if (res.passed) "✅ PASS" else "❌ FAIL"
            val ragBadge = if (res.ragEvaluation.hitExpectedTemplate) "HIT" else "MISS"
            val prioBadge = if (res.classificationEvaluation.priorityPassed) "MATCH" else "MISMATCH"
            println(" %-28s | %-8s | %-10s | %-10s | %6d ms".format(
                res.scenarioId.take(28),
                statusBadge,
                ragBadge,
                prioBadge,
                res.latencyMs
            ))
            if (!res.passed && res.failureReasons.isNotEmpty()) {
                for (reason in res.failureReasons) {
                    println("    └── ⚠️  $reason")
                }
            }
        }
        println(border)
        println()
    }
}

class AgentHarnessJsonReporter(
    private val outputDir: File = File("build/reports/agent-harness")
) : AgentHarnessReporter {

    private val objectMapper: ObjectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .enable(SerializationFeature.INDENT_OUTPUT)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    override fun report(summary: AgentEvaluationSummary) {
        outputDir.mkdirs()
        val outputFile = File(outputDir, "eval-report.json")
        objectMapper.writeValue(outputFile, summary)
        println("📄 JSON Evaluation Report generated at: ${outputFile.absolutePath}")
    }
}

class AgentHarnessHtmlReporter(
    private val outputDir: File = File("build/reports/agent-harness")
) : AgentHarnessReporter {

    override fun report(summary: AgentEvaluationSummary) {
        outputDir.mkdirs()
        val outputFile = File(outputDir, "index.html")
        val htmlContent = generateHtml(summary)
        outputFile.writeText(htmlContent)
        println("🌐 Interactive HTML Dashboard generated at: ${outputFile.absolutePath}")
    }

    private fun generateHtml(summary: AgentEvaluationSummary): String {
        val passColor = if (summary.passRatePercent >= 90.0) "#10b981" else if (summary.passRatePercent >= 70.0) "#f59e0b" else "#ef4444"

        val rowsHtml = summary.results.joinToString("\n") { res ->
            val statusClass = if (res.passed) "badge-pass" else "badge-fail"
            val statusText = if (res.passed) "PASS" else "FAIL"
            val ragClass = if (res.ragEvaluation.hitExpectedTemplate) "badge-pass" else "badge-fail"
            val classifClass = if (res.classificationEvaluation.priorityPassed && res.classificationEvaluation.categoryPassed) "badge-pass" else "badge-fail"
            val toolClass = if (res.toolExecutionEvaluation.executionSuccess) "badge-pass" else "badge-fail"

            val failReasonsHtml = if (res.failureReasons.isNotEmpty()) {
                """<div class="fail-reasons">${res.failureReasons.joinToString("<br>") { "⚠️ $it" }}</div>"""
            } else ""

            """
            <tr>
                <td><strong>${res.scenarioId}</strong><br><small class="text-muted">${res.scenarioName}</small></td>
                <td><span class="badge $statusClass">$statusText</span></td>
                <td><span class="badge $ragClass">${res.ragEvaluation.topTemplateType ?: "none"}</span></td>
                <td><span class="badge $classifClass">${res.classificationEvaluation.actualPriority} / ${res.classificationEvaluation.actualCategory}</span></td>
                <td><span class="badge $toolClass">${res.toolExecutionEvaluation.toolName ?: "none"}</span></td>
                <td>${res.latencyMs} ms</td>
            </tr>
            ${if (failReasonsHtml.isNotEmpty()) "<tr><td colspan='6'>$failReasonsHtml</td></tr>" else ""}
            """.trimIndent()
        }

        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>AI Agent Evaluation Harness Dashboard</title>
            <style>
                :root {
                    --bg: #0f172a;
                    --card-bg: #1e293b;
                    --text: #f8fafc;
                    --text-muted: #94a3b8;
                    --border: #334155;
                    --primary: #3b82f6;
                    --success: #10b981;
                    --warning: #f59e0b;
                    --danger: #ef4444;
                }
                body {
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                    background-color: var(--bg);
                    color: var(--text);
                    margin: 0;
                    padding: 24px;
                }
                .container { max-width: 1200px; margin: 0 auto; }
                header { margin-bottom: 24px; }
                h1 { margin: 0 0 8px 0; font-size: 28px; }
                .subtitle { color: var(--text-muted); font-size: 14px; }
                .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px; margin-bottom: 24px; }
                .card { background: var(--card-bg); border: 1px solid var(--border); border-radius: 12px; padding: 20px; }
                .metric-label { font-size: 13px; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.5px; }
                .metric-val { font-size: 32px; font-weight: 700; margin-top: 8px; color: var(--text); }
                .metric-val.highlight { color: $passColor; }
                table { width: 100%; border-collapse: collapse; background: var(--card-bg); border-radius: 12px; overflow: hidden; border: 1px solid var(--border); }
                th, td { padding: 14px 16px; text-align: left; border-bottom: 1px solid var(--border); font-size: 14px; }
                th { background: #182234; color: var(--text-muted); font-weight: 600; text-transform: uppercase; font-size: 12px; }
                .badge { display: inline-block; padding: 4px 10px; border-radius: 20px; font-size: 12px; font-weight: 600; }
                .badge-pass { background: rgba(16, 185, 129, 0.15); color: #34d399; border: 1px solid rgba(16, 185, 129, 0.3); }
                .badge-fail { background: rgba(239, 68, 68, 0.15); color: #f87171; border: 1px solid rgba(239, 68, 68, 0.3); }
                .text-muted { color: var(--text-muted); }
                .fail-reasons { background: rgba(239, 68, 68, 0.08); border-left: 3px solid var(--danger); padding: 8px 12px; margin: 4px 0; font-size: 13px; color: #fca5a5; }
            </style>
        </head>
        <body>
            <div class="container">
                <header>
                    <h1>🤖 AI Agent Evaluation Harness</h1>
                    <div class="subtitle">YouGile Alert Bot • Next-Gen Agent Verification Suite • ${summary.timestamp}</div>
                </header>

                <div class="grid">
                    <div class="card">
                        <div class="metric-label">Pass Rate</div>
                        <div class="metric-val highlight">${"%.1f".format(summary.passRatePercent)}%</div>
                        <small class="text-muted">${summary.passedScenarios} passed / ${summary.totalScenarios} total</small>
                    </div>
                    <div class="card">
                        <div class="metric-label">RAG Retrieval Hit Rate</div>
                        <div class="metric-val">${"%.1f".format(summary.ragHitRatePercent)}%</div>
                        <small class="text-muted">Top-K ground truth template match</small>
                    </div>
                    <div class="card">
                        <div class="metric-label">Classification Accuracy</div>
                        <div class="metric-val">${"%.1f".format(summary.priorityAccuracyPercent)}%</div>
                        <small class="text-muted">Priority & Category verification</small>
                    </div>
                    <div class="card">
                        <div class="metric-label">Tool Invocation Rate</div>
                        <div class="metric-val">${"%.1f".format(summary.toolInvocationRatePercent)}%</div>
                        <small class="text-muted">MCP create_yougile_task valid calls</small>
                    </div>
                    <div class="card">
                        <div class="metric-label">P95 Latency</div>
                        <div class="metric-val">${summary.p95LatencyMs} ms</div>
                        <small class="text-muted">Avg: ${"%.1f".format(summary.avgLatencyMs)} ms</small>
                    </div>
                </div>

                <h2>Scenario Benchmark Breakdown</h2>
                <table>
                    <thead>
                        <tr>
                            <th>Scenario</th>
                            <th>Status</th>
                            <th>RAG Template</th>
                            <th>Priority / Category</th>
                            <th>Tool Execution</th>
                            <th>Latency</th>
                        </tr>
                    </thead>
                    <tbody>
                        $rowsHtml
                    </tbody>
                </table>
            </div>
        </body>
        </html>
        """.trimIndent()
    }
}
