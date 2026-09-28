package xyz.xszq.bot.llm

import io.ktor.http.*
import xyz.xszq.bot.payload.llm.ContentPart
import xyz.xszq.bot.payload.llm.ImageUrl
import xyz.xszq.bot.payload.llm.InputAudio
import kotlin.io.encoding.Base64

/**
 * LLM 用户消息内容的 DSL Builder
 */
@Suppress("unused")
class UserMessageBuilder {
    internal val parts = mutableListOf<ContentPart>()

    fun text(content: String) {
        parts.add(ContentPart(type = "text", text = content))
    }

    fun image(url: String, detail: String? = null) {
        parts.add(
            ContentPart(
                type = "image_url",
                imageUrl = ImageUrl(url = url, detail = detail)
            )
        )
    }

    fun imageBase64(base64: String, mediaType: ContentType, detail: String? = null) {
        image("data:$mediaType;base64,$base64", detail)
    }

    fun image(data: ByteArray, mediaType: ContentType, detail: String? = null) {
        val base64 = Base64.encode(data)
        image("data:$mediaType;base64,$base64", detail)
    }

    fun audioBase64(base64: String, format: String = "wav") {
        parts.add(
            ContentPart(
                type = "input_audio",
                inputAudio = InputAudio(data = base64, format = format)
            )
        )
    }

    fun audio(data: ByteArray, format: String = "wav") {
        audioBase64(Base64.encode(data), format)
    }
}