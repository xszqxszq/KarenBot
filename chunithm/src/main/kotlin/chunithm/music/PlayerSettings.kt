package xyz.xszq.bot.chunithm.music

/**
 * 玩家自定义设置
 *
 * @property trophy 称号 ID
 * @property plate 名牌 ID
 * @property avatar 头像 ID
 */
data class PlayerSettings(
    val trophy: Int ?= null,
    val plate: Int ?= null,
    val avatar: Int ?= null
)