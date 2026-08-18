package xyz.xszq.bot.guess.touhou

import kotlinx.serialization.Serializable

@Serializable
data class TouhouMusics(
    val categories: List<Category>
)
