package xyz.xszq.bot.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DivingFishPlateRecord(
    val id: Int,
    val title: String,
    val level: String,
    @SerialName("level_index")
    val levelIndex: Int,
    val type: String,
    val achievements: Double,
    val fc: String,
    val fs: String,
)
