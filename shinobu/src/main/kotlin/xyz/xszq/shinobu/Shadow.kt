package xyz.xszq.shinobu

import kotlinx.serialization.Serializable

@Serializable(with = ShadowSerializer::class)
data class Shadow(
    val size: Double,
    val opacity: Double,
    val color: String
)
