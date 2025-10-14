package xyz.xszq.bot.theme

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Alignment {
    @SerialName("start")
    Start,
    @SerialName("center")
    Center,
    @SerialName("end")
    End,
    @SerialName("space-between")
    SpaceBetween,
    @SerialName("space-around")
    SpaceAround
}