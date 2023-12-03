package xyz.xszq.bot.maimai

import kotlinx.serialization.Serializable

@Serializable
data class TemplateProperties(
    val bg: String,
    val coverWidth: Int,
    val oldCols: Int = 0,
    val newCols: Int = 0,
    val gapX: Int = 0,
    val gapY: Int = 0,
    val pos: Map<String, ItemProperties>,
    val ellipsizeWidth: Int? = null
)
