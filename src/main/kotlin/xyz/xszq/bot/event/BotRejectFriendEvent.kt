package xyz.xszq.bot.event

import xyz.xszq.bot.Bot
import xyz.xszq.bot.User

/**
 * 用户取消机器人私聊主动消息权限时触发
 *
 * @param user 操作的用户
 */
class BotRejectFriendEvent(
    override val bot: Bot,
    override val eventId: String,
    override val user: User
): UserEvent