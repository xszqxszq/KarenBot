package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class MaimaiRatingInfo(
    val ratingList: List<MaimaiRatingRecord>,
    val newRatingList: List<MaimaiRatingRecord>
)
