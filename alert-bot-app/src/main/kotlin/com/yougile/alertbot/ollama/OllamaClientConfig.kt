package com.yougile.alertbot.ollama

import feign.Feign
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class OllamaClientConfig {

    @Value("\${ollama.base-url:http://localhost:11434}")
    private lateinit var baseUrl: String

    @Bean
    fun ollamaClient(): OllamaClient {
        return Feign.builder()
            .encoder(JacksonEncoder())
            .decoder(JacksonDecoder())
            .options(feign.Request.Options(
                java.time.Duration.ofMinutes(5),
                java.time.Duration.ofMinutes(5),
                false
            ))
            .target(OllamaClient::class.java, baseUrl)
    }
}
