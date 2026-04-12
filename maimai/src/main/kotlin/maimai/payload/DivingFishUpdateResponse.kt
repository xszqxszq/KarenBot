package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.Serializable

@Serializable
data class DivingFishUpdateResponse(
    val creates: Int,
    val message: String,
    val updates: Int
)
