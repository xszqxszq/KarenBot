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
import xyz.xszq.bot.payload.llm.*

/**
 * LLM 客户端
 *
 * @property config LLM 配置
 */
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

    private fun modelConfig(scene: String): LLMModelConfig =
        config.models[scene] ?: throw IllegalArgumentException("未配置 LLM 场景: $scene")

    /**
     * 进行 LLM 对话
     *
     * @param scene 该对话的场景名
     * @param block 构建对话的 DSL
     * @return 文本结果
     */
    suspend fun chat(scene: String, block: ChatBuilder.() -> Unit): String {
        val modelConfig = modelConfig(scene)
        val builder = ChatBuilder().apply(block)
        val response = client.post("${modelConfig.url}/chat/completions") {
            contentType(ContentType.Application.Json)
            headers {
                append(HttpHeaders.Authorization, "Bearer ${modelConfig.apikey}")
            }
            setBody(
                LLMRequest(
                    model = modelConfig.model,
                    messages = builder.messages,
                    temperature = modelConfig.temperature,
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

    /**
     * 计算文本的向量表示
     *
     * @param scene 该对话的场景名
     * @param input 要向量化的文本
     * @return 文本向量
     */
    suspend fun embed(scene: String, input: String): List<Float> =
        embed(scene, text = input, data = null, mediaType = null)

    /**
     * 计算图片数据的向量表示
     *
     * @param scene 该对话的场景名
     * @param data 图片数据
     * @param mediaType 图片媒体类型
     * @return 图片向量
     */
    suspend fun embed(
        scene: String,
        data: ByteArray,
        mediaType: String = "image/png",
    ): List<Float> = embed(scene, text = null, data = data, mediaType = mediaType)

    private suspend fun embed(
        scene: String,
        text: String?,
        data: ByteArray?,
        mediaType: String?,
    ): List<Float> {
        val modelConfig = modelConfig(scene)
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
        val response = client.post("${modelConfig.url}/embeddings/multimodal") {
            contentType(ContentType.Application.Json)
            headers {
                append(HttpHeaders.Authorization, "Bearer ${modelConfig.apikey}")
            }
            setBody(
                EmbeddingRequest(
                    model = modelConfig.model,
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