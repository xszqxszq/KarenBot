package xyz.xszq.bot.maimai.music

import kotlinx.serialization.Serializable

/**
 * 游戏版本
 *
 * @property id 版本 ID
 * @property name 版本名
 * @property version 版本代号
 */
@Serializable
data class GameVersion(
    val id: Int,
    val name: String,
    val version: Int
)