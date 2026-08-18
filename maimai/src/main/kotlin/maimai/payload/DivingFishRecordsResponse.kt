package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Suppress("unused")
@Serializable
data class DivingFishRecordsResponse(
    val username: String,
    val rating: Int,
    @SerialName("additional_rating")
    val additionalRating: Int,
    val nickname: String,
    val plate: String,
    val records: List<DivingFishRecord>
)