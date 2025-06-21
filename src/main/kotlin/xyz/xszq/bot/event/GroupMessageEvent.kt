package xyz.xszq.bot.event

import xyz.xszq.bot.Bot
import xyz.xszq.bot.Group
import xyz.xszq.bot.User
import xyz.xszq.bot.message.MessageChain

class GroupMessageEvent(
    override val bot: Bot,
    override val eventId: String,
    id: String,
    message: MessageChain,
    sender: User,
    override val group: Group,
    override var seq: Int = 1
): MessageEvent(
    bot = bot,
    eventId = eventId,
    id = id,
    message = message,
    sender = sender,
), GroupReplyAbleEvent