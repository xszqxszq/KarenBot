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
import xyz.xszq.bot.payload.llm.EmbeddingContentPart
import xyz.xszq.bot.payload.llm.EmbeddingImageUrl
import xyz.xszq.bot.payload.llm.EmbeddingRequest
import xyz.xszq.bot.payload.llm.EmbeddingResponse
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

    suspend fun embed(
        input: String,
        model: String? = null,
    ): List<Float> = embed(input, null, null, model)

    suspend fun embed(
        data: ByteArray,
        mediaType: String = "image/png",
        model: String? = null,
    ): List<Float> = embed(null, data, mediaType, model)

    private suspend fun embed(
        text: String?,
        data: ByteArray?,
        mediaType: String?,
        model: String?,
    ): List<Float> {
        val parts = mutableListOf<EmbeddingContentPart>()
        if (text != null) {
            parts.add(EmbeddingContentPart(type = "text", text = text))
        }
        if (data != null) {
            val base64 = java.util.Base64.getEncoder().encodeToString(data)
            parts.add(EmbeddingContentPart(
                type = "image_url",
                imageUrl = EmbeddingImageUrl(url = "data:$mediaType;base64,$base64"),
            ))
        }
        val response = client.post("${config.url}/embeddings/multimodal") {
            contentType(ContentType.Application.Json)
            headers {
                append(HttpHeaders.Authorization, "Bearer ${config.apikey}")
            }
            setBody(
                EmbeddingRequest(
                    model = model ?: config.model,
                    input = parts,
                )
            )
        }
        if (!response.status.isSuccess()) {
            when (response.status.value) {
                in 400..499 -> throw ClientRequestException(response, response.bodyAsText())
                else -> throw ServerResponseException(response, response.bodyAsText())
            }
        }
        return response.body<EmbeddingResponse>().data
            ?.embedding ?: emptyList()
    }
}
