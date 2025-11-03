package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class LocalCourseDamage(
    val perfect: Int,
    val great: Int,
    val good: Int,
    val miss: Int
)
