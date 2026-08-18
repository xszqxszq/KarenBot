package xyz.xszq.bot.meme.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MemeInfo(
    val key: String,
    val params: MemeParams,
    val keywords: List<String>,
    val shortcuts: List<MemeShortcut>,
    val tags: List<String>,
    @SerialName("date_created")
    val dateCreated: String = "",
    @SerialName("date_modified")
    val dateModified: String = "",
)