package xyz.xszq.bot.event

import xyz.xszq.bot.Bot
import xyz.xszq.bot.Group
import xyz.xszq.bot.Member

/**
 * 用户关闭机器人群聊消息接受权限事件
 *
 * @param group 取消授权的群组
 * @param operator 操作的用户
 */
class BotRejectGroupEvent(
    override val bot: Bot,
    override val eventId: String,
    override val group: Group,
    val operator: Member
): GroupEvent