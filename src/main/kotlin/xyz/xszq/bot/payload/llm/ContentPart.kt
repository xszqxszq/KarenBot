package xyz.xszq.bot.payload.llm

import kotlinx.serialization.Serializable

@Serializable
data class ContentPart(
    val type: String,
    val text: String? = null,
    val image_url: ImageUrl? = null,
)
