package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.Serializable

@Serializable
data class DivingFishMusicChart(
    val notes: List<Int>,
    val charter: String
)