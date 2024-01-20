package xyz.xszq.bot.rhythmgame.chunithm.payload

import kotlinx.serialization.Serializable

@Serializable
data class ChartInfo(
    val combo: Int,
    val charter: String
)
