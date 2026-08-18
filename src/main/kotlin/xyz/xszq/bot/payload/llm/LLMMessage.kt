package xyz.xszq.bot.payload.llm

import kotlinx.serialization.Serializable

@Serializable
data class LLMMessage(
    val role: String,
    @Serializable(with = MessageContentSerializer::class)
    val content: MessageContent,
) {
    fun contentAsText(): String? = when (content) {
        is MessageContentSingle -> content.text
        is MessageContentMulti -> content.parts
            .firstOrNull { it.type == "text" }?.text
    }
}