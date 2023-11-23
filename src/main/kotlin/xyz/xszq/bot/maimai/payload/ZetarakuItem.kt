package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.Serializable

@Serializable
data class ZetarakuItem(
    val title: String,
    val imageName: String,
    val artist: String
)
