package xyz.xszq.bot.meme.sekai

import kotlinx.serialization.Serializable

@Serializable
data class SekaiCharacter(
    val id: String,
    val name: String,
    val character: String,
    val img: String,
    val color: String,
    val defaultText: SekaiText
)
