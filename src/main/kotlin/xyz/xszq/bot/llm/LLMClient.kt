package xyz.xszq.bot.llm

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import xyz.xszq.bot.payload.llm.LLMRequest
import xyz.xszq.bot.payload.llm.LLMResponse

class LLMClient(
    private val config: LLMConfig,
    private val client: HttpClient = createDefaultClient(),
) {
    companion object {
        fun createDefaultClient() = HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    explicitNulls = false
                })
            }
        }
    }

    suspend fun chat(block: ChatBuilder.() -> Unit): String {
        val builder = ChatBuilder().apply(block)
        val response = client.post("${config.url}/chat/completions") {
            contentType(ContentType.Application.Json)
            headers {
                append(HttpHeaders.Authorization, "Bearer ${config.apikey}")
            }
            setBody(
                LLMRequest(
                    model = config.model,
                    messages = builder.messages,
                    temperature = config.temperature,
                    thinking = builder.thinking,
                    responseFormat = builder.responseFormat,
                )
            )
        }
        if (!response.status.isSuccess()) {
            when (response.status.value) {
                in 400..499 -> throw ClientRequestException(response, response.bodyAsText())
                else -> throw ServerResponseException(response, response.bodyAsText())
            }
        }
        return response.body<LLMResponse>().choices
            .firstOrNull()?.message?.contentAsText() ?: ""
    }
}
