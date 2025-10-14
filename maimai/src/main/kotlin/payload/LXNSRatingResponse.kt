package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class LXNSRatingResponse(
    val standard: List<LXNSScore>,
    val dx: List<LXNSScore>
)
