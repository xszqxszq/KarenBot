package xyz.xszq.bot.meme.payload

import kotlinx.serialization.Serializable

@Serializable
data class MemeUpload(
    val type: String,
    val url: String ?= null,
    val path: String ?= null,
    val data: String ?= null,
)
