package com.yougile.alertbot.config

import org.springframework.ai.ollama.api.OllamaApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.client.ReactorClientHttpRequestFactory
import org.springframework.web.client.RestClient
import reactor.netty.http.client.HttpClient
import java.time.Duration

@Configuration
class McpClientConfig {

    @Bean
    @Primary
    fun ollamaApi(): OllamaApi {
        val httpClient = HttpClient.create()
            .responseTimeout(Duration.ofMinutes(5))
        val restClient = RestClient.builder()
            .requestFactory(ReactorClientHttpRequestFactory(httpClient))
            .build()
        return OllamaApi.builder()
            .baseUrl("http://localhost:11434")
            .restClientBuilder(restClient.mutate())
            .build()
    }
}
