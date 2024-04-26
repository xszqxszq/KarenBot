package xyz.xszq.otomadbot.image

import kotlinx.serialization.Serializable

@Serializable
data class MemeArgument(
    val name: String,
    val type: String,
    val description: String,
    val default: String,
    val enum: List<String>? = null
)
