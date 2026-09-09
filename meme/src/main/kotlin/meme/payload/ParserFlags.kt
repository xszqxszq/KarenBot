package xyz.xszq.bot.meme.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 模板的文本参数
 *
 * @property short 是否为短参数
 * @property long 是否为长参数
 * @property shortAliases 短参数别名
 * @property longAliases 长参数别名
 */
@Serializable
data class ParserFlags(
    val short: Boolean,
    val long: Boolean,
    @SerialName("short_aliases")
    val shortAliases: List<String>,
    @SerialName("long_aliases")
    val longAliases: List<String>
)