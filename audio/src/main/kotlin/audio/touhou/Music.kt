package xyz.xszq.bot.audio.touhou

import kotlinx.serialization.Serializable

/**
 * 东方原曲
 *
 * @property name 中文名
 * @property jpn 日文名
 * @property aliases 别名
 */
@Serializable
data class Music(
    val id: Int,
    val name: String,
    val jpn: String,
    val file: String,
    val aliases: List<String>
)