package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.Serializable

@Serializable
data class DivingFishRecordsBests(
    val best: List<DivingFishRecord>
)
