package xyz.xszq.bot.component

import kotlinx.serialization.Serializable

@Serializable
data class Tag(
    val name: String,
    val aliases: List<String>,
    val musics: List<Int>
)