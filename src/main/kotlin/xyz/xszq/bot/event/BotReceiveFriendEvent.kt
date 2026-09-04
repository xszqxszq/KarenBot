package xyz.xszq.bot.event

import xyz.xszq.bot.Bot
import xyz.xszq.bot.User

/**
 * 用户开启机器人私聊主动消息权限事件
 *
 * @param user 操作的用户
 */
class BotReceiveFriendEvent(
    override val bot: Bot,
    override val eventId: String,
    override val user: User
): UserEvent