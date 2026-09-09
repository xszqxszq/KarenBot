package xyz.xszq.bot.meme.payload

import kotlinx.serialization.Serializable

/**
 * 表情包模板的快捷指令
 *
 * @property pattern 匹配的正则表达式
 * @property texts 默认文本
 * @property options 快捷指令的可选参数
 */
@Serializable
data class MemeShortcut(
    val pattern: String,
    val humanized: String ?= null,
    val names: List<String>,
    val texts: List<String>,
    val options: Map<String, String>
)