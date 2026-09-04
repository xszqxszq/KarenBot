package xyz.xszq.bot.payload.llm

/**
 * 多模态消息内容
 */
data class MessageContentMulti(val parts: List<ContentPart>) : MessageContent