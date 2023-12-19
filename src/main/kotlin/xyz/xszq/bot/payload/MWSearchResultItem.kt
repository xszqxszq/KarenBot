package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class MWSearchResultItem(
    val title: String,
    val size: Int,
    val snippet: String
)
