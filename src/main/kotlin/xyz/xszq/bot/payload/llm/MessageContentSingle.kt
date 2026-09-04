package xyz.xszq.bot.payload.llm

/**
 * LLM 纯文本消息内容
 */
@JvmInline
value class MessageContentSingle(val text: String) : MessageContent