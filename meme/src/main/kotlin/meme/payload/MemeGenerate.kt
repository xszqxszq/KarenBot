package xyz.xszq.bot.meme.payload

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

/**
 * 表情生成请求
 */
@Serializable
data class MemeGenerate(
    val images: List<MemeImage>,
    val texts: List<String>,
    val options: Map<String, JsonPrimitive>
)