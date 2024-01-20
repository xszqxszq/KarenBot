package xyz.xszq.bot.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MWExtractPage(
    @SerialName("pageid")
    val pageId: Int? = null,
    val title: String,
    val extract: String? = null,
    val missing: String? = null
)
