package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.Serializable

/**
 * 本地头像信息
 *
 * @property id 头像 ID
 * @property filename 文件名
 * @property name 头像名称
 * @property genre 头像类别
 * @property hint 获取条件
 */
@Serializable
data class LocalIconInfo(
    val id: Int,
    val filename: String,
    val name: String,
    val genre: String,
    val hint: String
)