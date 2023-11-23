package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BriefScore(
    val achievements: Double,
    val fc: String,
    val fs: String,
    val id: Int,
    val level: String,
    @SerialName("level_index")
    val levelIndex: Int,
    val title: String,
    val type: String
)
