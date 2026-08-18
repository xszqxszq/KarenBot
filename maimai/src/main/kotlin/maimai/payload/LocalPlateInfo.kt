package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.Serializable

@Serializable
data class LocalPlateInfo(
    val id: Int,
    val filename: String,
    val name: String,
    val genre: String,
    val hint: String,
    val requires: List<Int>,
    val remasters: List<Int>
)