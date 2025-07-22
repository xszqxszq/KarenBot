package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class MemeGenerate(
    val images: List<MemeImage>,
    val texts: List<String>,
    val options: Map<String, JsonPrimitive>
)