package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DivingFishRecordSimple(
    val title: String,
    val achievements: Double,
    val dxScore: Int,
    val fc: String,
    val fs: String,
    @SerialName("level_index")
    val levelIndex: Int,
    val type: String
)