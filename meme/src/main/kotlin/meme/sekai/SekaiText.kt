package xyz.xszq.bot.meme.sekai

import kotlinx.serialization.Serializable

@Serializable
data class SekaiText(
    val text: String,
    val x: Int,
    val y: Int,
    val r: Int,
    val s: Int
)
