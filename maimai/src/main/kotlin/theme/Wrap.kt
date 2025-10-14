package xyz.xszq.bot.theme

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Wrap {
    @SerialName("no-wrap")
    NoWrap,
    @SerialName("wrap")
    Wrap
}