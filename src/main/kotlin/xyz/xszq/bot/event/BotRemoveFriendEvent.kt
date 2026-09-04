package xyz.xszq.bot.event

import xyz.xszq.bot.Bot
import xyz.xszq.bot.User

/**
 * 机器人被移除好友事件
 *
 * @param user 移除机器人的用户
 */
class BotRemoveFriendEvent(
    override val bot: Bot,
    override val eventId: String,
    override val user: User
): UserEvent