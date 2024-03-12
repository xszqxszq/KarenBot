package xyz.xszq.bot.rhythmgame.lxns.payload

import kotlinx.serialization.Serializable

@Serializable
data class Collection(
    val id: Int,
    val name: String,
    val color: String
)
