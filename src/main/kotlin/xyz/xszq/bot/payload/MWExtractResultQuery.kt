package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class MWExtractResultQuery(
    val pages: Map<String, MWExtractPage>
)
