package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class DivingFishUpdateResponse(
    val message: String,
    val creates: Int ?= null,
    val updates: Int ?= null
)
