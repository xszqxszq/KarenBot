package xyz.xszq.bot.meme.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 表情包模板信息
 *
 * @property key 模板 ID
 * @property keywords 关键词
 * @property shortcuts 快捷指令
 */
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