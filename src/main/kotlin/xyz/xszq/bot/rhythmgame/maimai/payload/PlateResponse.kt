package xyz.xszq.bot.rhythmgame.maimai.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlateResponse(
    @SerialName("verlist")
    val verList: List<BriefScore>
)
