package xyz.xszq.bot.payload.llm

import kotlinx.serialization.Serializable

/**
 * LLM 消息内容
 */
@Serializable(with = MessageContentSerializer::class)
sealed interface MessageContent