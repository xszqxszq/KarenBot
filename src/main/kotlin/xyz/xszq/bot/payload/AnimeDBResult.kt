package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class AnimeDBResult(
    val code: Int,
    val data: List<AnimeDBItem>,
    val ai: Boolean? = false
)
