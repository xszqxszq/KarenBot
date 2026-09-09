package xyz.xszq.bot.maimai.music

/**
 * 玩家信息
 *
 * @property nickname 玩家昵称
 * @property rating 玩家 Rating
 * @property course 段位
 */
data class PlayerInfo(
    val nickname: String = "",
    val rating: Int = 0,
    val course: Int = 0
)