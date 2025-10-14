package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class MaimaiRegionResponse(
    val region: String,
    val playCount: Int,
    val created: String
)
