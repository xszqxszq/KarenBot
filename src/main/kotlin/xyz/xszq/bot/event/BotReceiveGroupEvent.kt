package xyz.xszq.bot.event

import xyz.xszq.bot.Bot
import xyz.xszq.bot.Group
import xyz.xszq.bot.Member
import xyz.xszq.bot.User

class BotReceiveGroupEvent(
    override val bot: Bot,
    override val eventId: String,
    override val group: Group,
    val operator: Member
): GroupEvent