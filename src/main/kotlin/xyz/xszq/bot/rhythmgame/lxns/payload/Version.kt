package xyz.xszq.bot.rhythmgame.lxns.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Version(
    val id: Int,
    val title: String,
    val version: Int
)
