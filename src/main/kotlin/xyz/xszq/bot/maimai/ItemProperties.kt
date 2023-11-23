package xyz.xszq.bot.maimai

import kotlinx.serialization.Serializable

@Serializable
data class ItemProperties(
    val bg: String,
    val coverWidth: Int,
    val coverRatio: Double,
    val oldCols: Int,
    val newCols: Int,
    val gapX: Int,
    val gapY: Int,
    val pos: Map<String, ItemPosition>
)
