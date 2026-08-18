package xyz.xszq.bot.payload.llm

import kotlinx.serialization.Serializable

@Serializable
data class ImageUrl(
    val url: String,
    val detail: String? = null,
)