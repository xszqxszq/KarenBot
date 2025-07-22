package xyz.xszq.bot.touhou

import kotlinx.serialization.Serializable

@Serializable
data class TouhouMusics(
    val categories: List<Category>
)
