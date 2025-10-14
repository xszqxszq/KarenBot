package xyz.xszq.bot.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DivingFishPlateResponse(
    @SerialName("verlist")
    val verList: List<DivingFishPlateRecord>
)
