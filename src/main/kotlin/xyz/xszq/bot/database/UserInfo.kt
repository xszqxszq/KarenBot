package xyz.xszq.bot.database

/**
 * 用户信息
 *
 * @property id 用户 OpenID
 * @property username 用户昵称
 * @property isBot 是否为机器人
 */
data class UserInfo(
    val id: String,
    val username: String,
    val isBot: Boolean
)