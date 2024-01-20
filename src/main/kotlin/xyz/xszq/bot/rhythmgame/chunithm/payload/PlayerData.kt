package xyz.xszq.bot.rhythmgame.chunithm.payload

import kotlinx.serialization.Serializable

@Serializable
data class PlayerData(
    val rating: Double,
    val records: RecordList,
    val username: String
)
