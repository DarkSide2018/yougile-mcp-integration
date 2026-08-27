package com.yougile.alertbot.harness.dataset

import com.yougile.alertbot.harness.model.AlertEvalScenario
import com.yougile.alertbot.harness.model.ExpectedOutcome
import com.yougile.alertbot.model.Alert
import com.yougile.alertbot.model.AlertPriority
import java.time.Instant

object GoldenAlertDataset {

    val allScenarios: List<AlertEvalScenario> = listOf(
        AlertEvalScenario(
            id = "SCENARIO-001-CPU-CRITICAL",
            name = "Critical Database CPU Spike",
            description = "High load spike on primary PostgreSQL server exceeding 95% CPU threshold for >5min",
            alert = Alert(
                text = """
                    CRITICAL: Host db-master-01 CPU utilization is 96.4% (Threshold: 90%)
                    Service: postgresql-cluster
                    Cluster: production-eu-central
                    Metric: node_cpu_seconds_total
                    Active connections: 840/1000
                    Top consumer: postgres: app_user billing_db [local] SELECT
                    Timestamp: ${Instant.now()}
                """.trimIndent(),
                priority = AlertPriority.CRITICAL,
                category = "infrastructure",
                source = "Prometheus/Alertmanager"
            ),
            expected = ExpectedOutcome(
                acceptablePriorities = setOf(AlertPriority.CRITICAL, AlertPriority.HIGH),
                acceptableCategories = setOf("infrastructure", "database", "server"),
                expectedTemplateType = "cpu",
                requiredTitleKeywords = listOf("CPU", "db-master-01"),
                requiredDescriptionKeywords = listOf("96", "postgresql-cluster")
            ),
            tags = listOf("infrastructure", "database", "cpu", "critical")
        ),

        AlertEvalScenario(
            id = "SCENARIO-002-MEM-LEAK-HIGH",
            name = "JVM Heap Memory Leak in Billing Service",
            description = "Sustained memory growth in Spring Boot service leading to JVM GC pauses and impending OOM",
            alert = Alert(
                text = """
                    HIGH: JVM Memory Leak detected on billing-service-pod-78dfb8b4c-kx8lp
                    Host: k8s-node-worker-04
                    Heap Used: 3.8GB / 4.0GB (95.0%)
                    Growth Rate: 45 MB/min sustained for 45 minutes
                    OldGen Usage: 98%
                    GC overhead limit exceeded: GC time > 20% in last 5m
                    Timestamp: ${Instant.now()}
                """.trimIndent(),
                priority = AlertPriority.HIGH,
                category = "infrastructure",
                source = "K8s Prometheus"
            ),
            expected = ExpectedOutcome(
                acceptablePriorities = setOf(AlertPriority.HIGH, AlertPriority.CRITICAL),
                acceptableCategories = setOf("infrastructure", "application", "jvm"),
                expectedTemplateType = "memory",
                requiredTitleKeywords = listOf("Memory", "billing-service"),
                requiredDescriptionKeywords = listOf("Heap", "45 MB/min")
            ),
            tags = listOf("memory", "jvm", "application", "high")
        ),

        AlertEvalScenario(
            id = "SCENARIO-003-DISK-FULL-DB",
            name = "Database Data Partition Disk Exhaustion",
            description = "PostgreSQL data volume /var/lib/postgresql/data is at 98% disk capacity with only 4GB free",
            alert = Alert(
                text = """
                    CRITICAL: Disk Space Alert on pg-replica-02
                    Mount: /var/lib/postgresql/data
                    Used: 490GB / 500GB (98.0%)
                    Free: 10GB remaining
                    Est. Time to Full: 3 hours at current WAL generation rate
                    Action required: WAL purge or volume resize immediately
                    Timestamp: ${Instant.now()}
                """.trimIndent(),
                priority = AlertPriority.CRITICAL,
                category = "infrastructure",
                source = "Node Exporter"
            ),
            expected = ExpectedOutcome(
                acceptablePriorities = setOf(AlertPriority.CRITICAL, AlertPriority.HIGH),
                acceptableCategories = setOf("infrastructure", "database", "storage"),
                expectedTemplateType = "disk",
                requiredTitleKeywords = listOf("Disk", "pg-replica-02"),
                requiredDescriptionKeywords = listOf("/var/lib/postgresql/data", "98.0%")
            ),
            tags = listOf("disk", "storage", "database", "critical")
        ),

        AlertEvalScenario(
            id = "SCENARIO-004-SERVICE-OUTAGE",
            name = "Authentication Service 503 Outage",
            description = "Auth Gateway health probe failing with HTTP 503 and Connection Refused on internal port 8080",
            alert = Alert(
                text = """
                    CRITICAL: Service auth-gateway on host auth-app-01 is DOWN!
                    Health check failed: HTTP 503 Service Unavailable / Connection Refused
                    Endpoint: https://auth.internal.yougile.com/actuator/health
                    Consecutive failures: 5
                    Impact: Users cannot authenticate or issue API tokens
                    Downtime Started: ${Instant.now()}
                """.trimIndent(),
                priority = AlertPriority.CRITICAL,
                category = "application",
                source = "Blackbox Exporter"
            ),
            expected = ExpectedOutcome(
                acceptablePriorities = setOf(AlertPriority.CRITICAL),
                acceptableCategories = setOf("application", "infrastructure", "service"),
                expectedTemplateType = "service",
                requiredTitleKeywords = listOf("auth-gateway", "DOWN"),
                requiredDescriptionKeywords = listOf("503", "auth-app-01")
            ),
            tags = listOf("service", "outage", "application", "critical")
        ),

        AlertEvalScenario(
            id = "SCENARIO-005-NETWORK-DEGRADATION",
            name = "Network Gateway Packet Loss & High Latency",
            description = "Border router packet drop rate reached 12% causing cross-region API timeouts",
            alert = Alert(
                text = """
                    WARNING: High Packet Loss on edge-router-fra-01
                    Interface: eth0 (uplink-transit-01)
                    Packet loss: 12.5% over 10m window
                    RTT P99: 340ms (Baseline: 25ms)
                    Affected traffic: Europe-West to Europe-Central interconnect
                    Timestamp: ${Instant.now()}
                """.trimIndent(),
                priority = AlertPriority.HIGH,
                category = "network",
                source = "Network Monitor"
            ),
            expected = ExpectedOutcome(
                acceptablePriorities = setOf(AlertPriority.HIGH, AlertPriority.MEDIUM),
                acceptableCategories = setOf("network", "infrastructure"),
                expectedTemplateType = "default",
                requiredTitleKeywords = listOf("Packet Loss", "edge-router"),
                requiredDescriptionKeywords = listOf("12.5%", "RTT")
            ),
            tags = listOf("network", "high", "infrastructure")
        ),

        AlertEvalScenario(
            id = "SCENARIO-006-SECURITY-INCIDENT",
            name = "SSH Brute Force Attack on Bastion Host",
            description = "Intrusion detection system caught 800+ failed SSH login attempts from suspicious IP range",
            alert = Alert(
                text = """
                    SECURITY ALERT: SSH Brute Force detected on bastion-eu-01
                    Source IP: 198.51.100.42 (Multiple ASN attempts)
                    Failed login attempts: 850 in 3 minutes
                    Target usernames: root, admin, ubuntu, deploy
                    Fail2ban status: triggered, IP banned for 24h
                    Timestamp: ${Instant.now()}
                """.trimIndent(),
                priority = AlertPriority.CRITICAL,
                category = "security",
                source = "Wazuh SIEM"
            ),
            expected = ExpectedOutcome(
                acceptablePriorities = setOf(AlertPriority.CRITICAL, AlertPriority.HIGH),
                acceptableCategories = setOf("security", "infrastructure"),
                expectedTemplateType = "default",
                requiredTitleKeywords = listOf("SSH", "bastion-eu-01"),
                requiredDescriptionKeywords = listOf("198.51.100.42", "Brute Force")
            ),
            tags = listOf("security", "critical")
        ),

        AlertEvalScenario(
            id = "SCENARIO-007-RUSSIAN-ALERT",
            name = "Russian Language Alert: Redis OOM",
            description = "Alert received in Russian language describing Redis cache OOM killer termination",
            alert = Alert(
                text = """
                    КРИТИЧЕСКИЙ СБОЙ: Процесс Redis OOM Killer на redis-cache-cluster-03
                    Хост: redis-prod-03
                    Память: 16.0GB / 16.0GB (100%)
                    maxmemory-policy: noeviction
                    Приложение: Сессии пользователей и очереди кэша сброшены
                    Время: ${Instant.now()}
                """.trimIndent(),
                priority = AlertPriority.CRITICAL,
                category = "infrastructure",
                source = "Telegram Bot / Alert Relay"
            ),
            expected = ExpectedOutcome(
                acceptablePriorities = setOf(AlertPriority.CRITICAL, AlertPriority.HIGH),
                acceptableCategories = setOf("infrastructure", "database", "memory", "general"),
                expectedTemplateType = "memory",
                requiredTitleKeywords = listOf("Redis", "redis-prod-03"),
                requiredDescriptionKeywords = listOf("16.0GB", "redis-cache-cluster-03")
            ),
            tags = listOf("multilingual", "russian", "memory", "redis")
        ),

        AlertEvalScenario(
            id = "SCENARIO-008-LOW-PRIO-BACKUP-DELAY",
            name = "Routine Nightly Backup Delay",
            description = "Informational alert about nightly cold backup completing 15 minutes behind schedule",
            alert = Alert(
                text = """
                    INFO: Nightly database backup took longer than expected
                    Target: s3://yougile-backup-vault/pg-prod-dump-2026.sql.gz
                    Duration: 75 minutes (Threshold: 60 minutes)
                    Status: COMPLETED_SUCCESSFULLY
                    Size: 142 GB
                    No data lost or corrupted.
                    Timestamp: ${Instant.now()}
                """.trimIndent(),
                priority = AlertPriority.LOW,
                category = "database",
                source = "Backup Daemon"
            ),
            expected = ExpectedOutcome(
                acceptablePriorities = setOf(AlertPriority.LOW, AlertPriority.MEDIUM),
                acceptableCategories = setOf("database", "general", "infrastructure"),
                expectedTemplateType = "default",
                requiredTitleKeywords = listOf("Backup"),
                requiredDescriptionKeywords = listOf("s3://yougile-backup-vault", "75 minutes")
            ),
            tags = listOf("info", "low", "backup")
        )
    )

    fun getScenario(id: String): AlertEvalScenario? = allScenarios.find { it.id.equals(id, ignoreCase = true) }
}
