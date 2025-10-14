package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class MaimaiRatingRecord(
    val musicId: Int,
    val level: Int,
    val romVersion: Int,
    val achievement: Int
)
