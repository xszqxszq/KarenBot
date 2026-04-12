package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DivingFishRatingResponse(
    val username: String,
    val rating: Int,
    @SerialName("additional_rating")
    val additionalRating: Int,
    val nickname: String,
    val plate: String ?= null,
    val charts: DivingFishCharts
)
