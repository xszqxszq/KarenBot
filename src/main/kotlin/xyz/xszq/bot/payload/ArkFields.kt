package xyz.xszq.bot.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed interface ArkFields

@Serializable
data class ArkMiniApp(
    val preview: String = "",
    val source: String = "",
    @SerialName("source_logo")
    val sourceLogo: String = "",
    val title: String = ""
): ArkFields

@Serializable
data class ArkMixed(
    val desc: String = "",
    @SerialName("jump_url")
    val jumpUrl: String = "",
    val tag: String = "",
    val title: String = ""
): ArkFields
