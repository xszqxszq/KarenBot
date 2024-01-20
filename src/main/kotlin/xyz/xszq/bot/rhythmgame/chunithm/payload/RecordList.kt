package xyz.xszq.bot.rhythmgame.chunithm.payload

import kotlinx.serialization.Serializable

@Serializable
data class RecordList(
    val best: List<PlayScore>,
    val r10: List<PlayScore>
)
