package xyz.xszq.bot.event

import xyz.xszq.bot.Bot
import xyz.xszq.bot.User

/**
 * 机器人被添加好友事件
 *
 * @param user 添加机器人的用户
 */
class BotAddFriendEvent(
    override val bot: Bot,
    override val eventId: String,
    override val user: User
): UserEvent