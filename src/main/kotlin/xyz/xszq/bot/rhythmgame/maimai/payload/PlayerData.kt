package xyz.xszq.bot.rhythmgame.maimai.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlayerData(
    val nickname: String,
    val rating: Int,
    @SerialName("additional_rating")
    val additionalRating: Int ?= null,
    val username: String,
    val plate: String,
    val charts: Map<String, List<PlayScore>>
)