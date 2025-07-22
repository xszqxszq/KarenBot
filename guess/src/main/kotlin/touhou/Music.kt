package xyz.xszq.bot.touhou

import kotlinx.serialization.Serializable

@Serializable
data class Music(
    val id: Int,
    val name: String,
    val jpn: String,
    val file: String,
    val aliases: List<String>
)
