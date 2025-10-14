package xyz.xszq.bot.theme

import kotlinx.serialization.Serializable

@Serializable(with = BorderSerializer::class)
data class Border(
    val size: Double,
    val color: String
)