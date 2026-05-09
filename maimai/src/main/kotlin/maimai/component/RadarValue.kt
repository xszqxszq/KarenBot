package xyz.xszq.bot.maimai.component

import kotlinx.serialization.Serializable

@Serializable
data class RadarValue(
    val notes: Double,
    val peak: Double,
    val stamina: Double,
    val slide: Double,
    val handTrip: Double
)