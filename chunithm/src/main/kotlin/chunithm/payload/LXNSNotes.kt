package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.Serializable

@Serializable
data class LXNSNotes(
    val total: Int,
    val tap: Int,
    val hold: Int,
    val slide: Int,
    val air: Int,
    val flick: Int
)
