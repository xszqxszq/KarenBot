package xyz.xszq.bot.rhythmgame.maimai.payload

import kotlinx.serialization.Serializable

@Serializable
data class ZetarakuItem(
    val title: String,
    val imageName: String,
    val artist: String
)
