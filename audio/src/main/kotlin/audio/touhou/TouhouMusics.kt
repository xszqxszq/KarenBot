package xyz.xszq.bot.audio.touhou

import kotlinx.serialization.Serializable

@Serializable
data class TouhouMusics(
    val categories: List<Category>
)