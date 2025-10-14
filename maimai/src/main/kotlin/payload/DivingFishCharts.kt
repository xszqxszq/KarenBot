package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class DivingFishCharts(
    val sd: List<DivingFishRecord>,
    val dx: List<DivingFishRecord>
)
