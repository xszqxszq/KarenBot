package xyz.xszq.bot.event

import xyz.xszq.bot.Bot
import xyz.xszq.bot.Group
import xyz.xszq.bot.Member

/**
 * 机器人退出群聊事件
 *
 * @param group 被移除的群组
 * @param operator 移除机器人操作的成员
 */
class BotLeaveGroupEvent(
    override val bot: Bot,
    override val eventId: String,
    override val group: Group,
    val operator: Member
): GroupEvent