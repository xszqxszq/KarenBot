package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class DivingFishChartInfo(
    val notes: List<Int>,
    val charter: String
)
