package xyz.xszq.bot.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ArkMiniApp(
    val preview: String = "",
    val source: String = "",
    @SerialName("source_logo")
    val sourceLogo: String = "",
    val title: String = ""
) : ArkFields