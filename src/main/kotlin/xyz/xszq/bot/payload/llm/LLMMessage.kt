package xyz.xszq.bot.payload.llm

import kotlinx.serialization.Serializable

/**
 * LLM 对话消息
 */
@Serializable
data class LLMMessage(
    val role: String,
    @Serializable(with = MessageContentSerializer::class)
    val content: MessageContent,
) {
    /**
     * 消息中的文本内容
     *
     * @return 纯文本
     */
    fun contentAsText(): String? = when (content) {
        is MessageContentSingle -> content.text
        is MessageContentMulti -> content.parts
            .firstOrNull { it.type == "text" }?.text
    }
}