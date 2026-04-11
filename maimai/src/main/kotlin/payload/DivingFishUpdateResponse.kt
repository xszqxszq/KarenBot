package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class DivingFishUpdateResponse(
    val creates: Int,
    val message: String,
    val updates: Int
)
