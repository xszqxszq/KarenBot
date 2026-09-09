package xyz.xszq.bot.maimai.music

/**
 * 玩家自定义设置
 *
 * @property avatar 头像 ID
 * @property plate 牌子 ID
 */
data class PlayerSettings(
    val avatar: Int ?= null,
    val plate: Int ?= null
)