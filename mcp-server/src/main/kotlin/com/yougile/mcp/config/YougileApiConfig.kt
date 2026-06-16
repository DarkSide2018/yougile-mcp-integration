package com.yougile.mcp.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class YougileApiConfig {

    @Value("\${yougile.api.base-url}")
    private lateinit var baseUrl: String

    @Value("\${yougile.api.key}")
    private lateinit var apiKey: String

    @Bean
    fun yougileWebClient(): WebClient = WebClient.builder()
        .baseUrl(baseUrl)
        .defaultHeader("Content-Type", "application/json")
        .defaultHeader("Authorization", "Bearer $apiKey")
        .build()
}
