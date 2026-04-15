package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.Serializable

@Serializable
data class DivingFishRecordsResponse(
    val username: String,
    val nickname: String,
    val rating: Double,
    val records: DivingFishRecordsBests
)