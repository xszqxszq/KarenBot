package xyz.xszq.bot.event

import xyz.xszq.bot.Bot
import xyz.xszq.bot.Group
import xyz.xszq.bot.Member

/**
 * 机器人加入群聊事件
 *
 * @param group 加入的群组
 * @param operator 添加机器人操作的成员
 */
class BotJoinGroupEvent(
    override val bot: Bot,
    override val eventId: String,
    override val group: Group,
    val operator: Member
): GroupEvent