package xyz.xszq.bot.maimai.component

import kotlinx.serialization.Serializable

/**
 * 歌曲标签
 *
 * @property name 标签名
 * @property aliases 标签别名
 * @property musics 包含的歌曲 ID
 */
@Serializable
data class Tag(
    val name: String,
    val aliases: List<String>,
    val musics: List<Int>
)