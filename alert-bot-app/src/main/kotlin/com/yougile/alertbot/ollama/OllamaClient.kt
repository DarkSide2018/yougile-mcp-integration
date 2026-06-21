package com.yougile.alertbot.ollama

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import feign.Headers
import feign.RequestLine

@Headers("Content-Type: application/json")
interface OllamaClient {

    @RequestLine("POST /api/chat")
    fun chat(@JsonProperty request: ChatRequest): ChatResponse

    @RequestLine("POST /api/embed")
    fun embed(@JsonProperty request: EmbedRequest): EmbedResponse
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ChatRequest(
    @JsonProperty("model") val model: String,
    @JsonProperty("messages") val messages: List<Message>,
    @JsonProperty("stream") val stream: Boolean = false,
    @JsonProperty("options") val options: Map<String, Any>? = null
)

data class Message(
    @JsonProperty("role") val role: String,
    @JsonProperty("content") val content: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ChatResponse(
    @JsonProperty("model") val model: String? = null,
    @JsonProperty("message") val message: ResponseMessage? = null,
    @JsonProperty("done") val done: Boolean? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ResponseMessage(
    @JsonProperty("role") val role: String? = null,
    @JsonProperty("content") val content: String? = null,
    @JsonProperty("thinking") val thinking: String? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EmbedRequest(
    @JsonProperty("model") val model: String,
    @JsonProperty("input") val input: List<String>,
    @JsonProperty("keep_alive") val keepAlive: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EmbedResponse(
    @JsonProperty("embeddings") val embeddings: List<List<Double>>? = null
)
