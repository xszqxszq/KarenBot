package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.Serializable

/**
 * 本地牌子信息
 *
 * @property id 牌子 ID
 * @property filename 文件名
 * @property name 牌子名称
 * @property genre 牌子类型
 * @property hint 获取条件
 * @property requires 达成牌子所需的歌曲 ID 列表
 * @property remasters 达成牌子所需的白谱的歌曲 ID 列表
 */
@Serializable
data class LocalPlateInfo(
    val id: Int,
    val filename: String,
    val name: String,
    val genre: String,
    val hint: String,
    val requires: List<Int>,
    val remasters: List<Int>
)