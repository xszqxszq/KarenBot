package xyz.xszq.bot.event

import xyz.xszq.bot.Bot
import xyz.xszq.bot.Group
import xyz.xszq.bot.Member
import xyz.xszq.bot.User
import xyz.xszq.bot.message.MessageChain

/**
 * 群消息事件
 */
class GroupMessageEvent(
    override val bot: Bot,
    override val eventId: String,
    id: String,
    message: MessageChain,
    override val sender: Member,
    override val group: Group,
    override var seq: Int = 1,
    override val reference: MessageChain? = null,
    val mentions: List<User> = emptyList()
): MessageEvent(
    bot = bot,
    eventId = eventId,
    id = id,
    message = message,
    sender = sender,
    reference = reference
), GroupReplyAbleEvent