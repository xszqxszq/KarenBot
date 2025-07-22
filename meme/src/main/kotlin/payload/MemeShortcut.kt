package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class MemeShortcut(
    val pattern: String,
    val humanized: String ?= null,
    val names: List<String>,
    val texts: List<String>,
    val options: Map<String, String>
)
