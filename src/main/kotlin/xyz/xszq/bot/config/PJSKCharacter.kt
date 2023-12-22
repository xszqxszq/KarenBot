package xyz.xszq.bot.config

import kotlinx.serialization.Serializable

@Serializable
data class PJSKCharacter(
    val id: String,
    val name: String,
    val character: String,
    val img: String,
    val color: String,
    val defaultText: PJSKText
)
