package xyz.xszq.bot.chunithm.music

/**
 * 玩家信息
 *
 * @property nickname 玩家昵称
 * @property rating 玩家 Rating
 * @property level 玩家等级
 */
data class PlayerInfo(
    val nickname: String = "",
    val rating: Double = 0.0,
    val level: Int = 1
)