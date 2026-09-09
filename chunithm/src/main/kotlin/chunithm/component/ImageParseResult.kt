package xyz.xszq.bot.chunithm.component

import kotlinx.serialization.Serializable

/**
 * 识别出的歌曲成绩
 *
 * @property game 所属游戏
 * @property type 谱面类型
 * @property deluxeScore DX 分数
 */
@Serializable
data class ImageParseResult(
    val game: String = "",
    val title: String = "",
    val achievement: String = "",
    val difficulty: String = "",
    val combo: String = "",
    val sync: String = "",
    val type: String = "",
    val deluxeScore: Int = 0
)