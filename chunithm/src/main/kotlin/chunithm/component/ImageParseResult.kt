package xyz.xszq.bot.chunithm.component

import kotlinx.serialization.Serializable

@Serializable
data class ImageParseResult(
    val game: String = "",
    val title: String = "",
    val achievement: String = "",
    val difficulty: String = "",
    val combo: String = "",
    val sync: String = "",
    val type: String = "",
    val deluxeScore: Int = 0
)