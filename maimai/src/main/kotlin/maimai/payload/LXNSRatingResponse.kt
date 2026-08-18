package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.Serializable

@Serializable
data class LXNSRatingResponse(
    val standard: List<LXNSScore>,
    val dx: List<LXNSScore>
)