package xyz.xszq.bot.payload.llm

data class MessageContentMulti(val parts: List<ContentPart>) : MessageContent
