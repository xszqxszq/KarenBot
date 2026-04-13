package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LXNSRatingResponse(
    val bests: List<LXNSScore>,
    val selections: List<LXNSScore> = listOf(),
    @SerialName("new_bests")
    val newBests: List<LXNSScore>
)