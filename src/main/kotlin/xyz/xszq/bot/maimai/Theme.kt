package xyz.xszq.bot.maimai

import kotlinx.serialization.Serializable

@Serializable
data class Theme(
    val b50: ItemProperties,
    val scoreList: ItemProperties,
    val dsList: ItemProperties,
    val info: ItemProperties
)
