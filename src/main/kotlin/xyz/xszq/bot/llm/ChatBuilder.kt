package xyz.xszq.bot.llm

import xyz.xszq.bot.payload.llm.*

/**
 * LLM 对话的 DSL Builder
 *
 * 指定系统、用户消息等，且可设置思考模式与响应格式
 */
@Suppress("unused")
class ChatBuilder {
    internal val messages = mutableListOf<LLMMessage>()
    internal var thinking: LLMThinking? = LLMThinking("disabled")
    internal var responseFormat: ResponseFormat? = null

    fun system(content: String) {
        messages.add(LLMMessage("system", MessageContentSingle(content)))
    }

    fun assistant(content: String) {
        messages.add(LLMMessage("assistant", MessageContentSingle(content)))
    }

    fun user(content: String) {
        messages.add(LLMMessage("user", MessageContentSingle(content)))
    }

    fun user(block: UserMessageBuilder.() -> Unit) {
        val builder = UserMessageBuilder().apply(block)
        messages.add(LLMMessage("user", MessageContentMulti(builder.parts)))
    }

    fun thinking(enabled: Boolean) {
        thinking = LLMThinking(if (enabled) "enabled" else "disabled")
    }

    fun thinking(type: String) {
        thinking = LLMThinking(type)
    }

    fun responseFormat(type: String) {
        responseFormat = ResponseFormat(type)
    }
}