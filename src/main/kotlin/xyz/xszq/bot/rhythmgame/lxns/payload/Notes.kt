package xyz.xszq.bot.rhythmgame.lxns.payload

import kotlinx.serialization.Serializable

@Serializable
data class Notes(
    val total: Int,
    val tap: Int,
    val hold: Int,
    val slide: Int,
    val air: Int,
    val flick: Int
)
