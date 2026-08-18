package xyz.xszq.bot.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ArkMixed(
    val desc: String = "",
    @SerialName("jump_url")
    val jumpUrl: String = "",
    val tag: String = "",
    val title: String = ""
) : ArkFields