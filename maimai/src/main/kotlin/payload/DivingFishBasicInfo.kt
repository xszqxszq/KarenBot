package xyz.xszq.bot.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DivingFishBasicInfo(
    val title: String,
    val artist: String,
    val genre: String,
    val bpm: Int,
    val from: String,
    @SerialName("is_new")
    val isNew: Boolean
)
