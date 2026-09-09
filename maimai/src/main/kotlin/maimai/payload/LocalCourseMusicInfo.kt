package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.Serializable

/**
 * 本地段位考核中的歌曲信息
 *
 * @property id 歌曲 ID
 * @property name 歌曲名
 * @property difficulty 歌曲难度
 */
@Serializable
data class LocalCourseMusicInfo(
    val id: Int,
    val name: String = "",
    val difficulty: Int,
)