package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.Serializable

@Serializable
data class DivingFishRecords(
    val b30: List<DivingFishRecord> = listOf(),
    val n20: List<DivingFishRecord> = listOf(),
    val r10: List<DivingFishRecord> = listOf()
)