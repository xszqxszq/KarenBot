package xyz.xszq.bot.config

import kotlinx.serialization.Serializable

@Serializable
data class PJSKText(
    val text: String,
    val x: Int,
    val y: Int,
    val r: Int,
    val s: Int
)
