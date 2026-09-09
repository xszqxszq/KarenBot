package xyz.xszq.bot.chunithm.music

/**
 * 查询结果
 *
 * @property player 玩家信息
 * @property settings 玩家自定义设置
 */
sealed interface Response {
    val player: PlayerInfo
    var settings: PlayerSettings?
}