package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class MWExtractResult(
    val query: MWExtractResultQuery? = null
)
