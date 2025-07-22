package xyz.xszq.bot.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ParserFlags(
    val short: Boolean,
    val long: Boolean,
    @SerialName("short_aliases")
    val shortAliases: List<String>,
    @SerialName("long_aliases")
    val longAliases: List<String>
)
