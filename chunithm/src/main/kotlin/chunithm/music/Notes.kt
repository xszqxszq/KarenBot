package xyz.xszq.bot.chunithm.music

import kotlinx.serialization.Serializable

@Serializable
data class Notes(
    val total: Int = 0,
    val tap: Int = 0,
    val hold: Int = 0,
    val slide: Int = 0,
    val air: Int = 0,
    val flick: Int = 0
)