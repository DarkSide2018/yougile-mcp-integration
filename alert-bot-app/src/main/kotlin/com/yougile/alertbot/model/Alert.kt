package com.yougile.alertbot.model

import java.time.Instant

data class Alert(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val source: String = "telegram",
    val timestamp: Instant = Instant.now(),
    val priority: AlertPriority = AlertPriority.MEDIUM,
    val category: String = "general"
)

enum class AlertPriority {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW
}
