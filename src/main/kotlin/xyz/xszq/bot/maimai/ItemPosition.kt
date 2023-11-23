package xyz.xszq.bot.maimai

import kotlinx.serialization.Serializable

@Serializable
data class ItemPosition(
    val fontName: String = "",
    val size: Int = 0,
    val x: Int,
    val y: Int,
    val scale: Double = 1.0
)
