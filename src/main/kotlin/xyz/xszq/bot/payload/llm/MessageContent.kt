package xyz.xszq.bot.payload.llm

import kotlinx.serialization.Serializable

@Serializable(with = MessageContentSerializer::class)
sealed interface MessageContent
