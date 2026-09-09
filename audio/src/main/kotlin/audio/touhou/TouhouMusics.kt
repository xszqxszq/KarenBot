package xyz.xszq.bot.audio.touhou

import kotlinx.serialization.Serializable

/**
 * 东方原曲数据
 */
@Serializable
data class TouhouMusics(
    val categories: List<Category>
)