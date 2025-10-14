package xyz.xszq.bot.theme

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Direction {
    @SerialName("row")
    Row,
    @SerialName("column")
    Column
}