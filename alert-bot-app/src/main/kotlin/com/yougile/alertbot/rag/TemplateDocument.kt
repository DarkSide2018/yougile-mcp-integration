package com.yougile.alertbot.rag

data class TemplateDocument(
    val id: String,
    val type: String,
    val priority: String,
    val category: String,
    val tags: List<String>,
    val content: String
)
