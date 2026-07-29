package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.Serializable

@Serializable
data class DivingFishCharts(
    val sd: List<DivingFishRecord>,
    val dx: List<DivingFishRecord>
)
