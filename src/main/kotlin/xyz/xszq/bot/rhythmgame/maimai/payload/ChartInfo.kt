package xyz.xszq.bot.rhythmgame.maimai.payload

import kotlinx.serialization.Serializable

@Serializable
data class ChartInfo(
    val notes: List<Int>,
    val charter: String
)
