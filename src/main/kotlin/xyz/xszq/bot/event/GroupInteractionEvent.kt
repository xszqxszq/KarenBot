package xyz.xszq.bot.event

import xyz.xszq.bot.Bot
import xyz.xszq.bot.Group
import xyz.xszq.bot.User

class GroupInteractionEvent(
    override val bot: Bot,
    override val eventId: String,
    id: String,
    data: String,
    button: String,
    sender: User,
    override val group: Group,
    override var seq: Int = 1
): InteractionEvent(
    bot = bot,
    eventId = eventId,
    id = id,
    data = data,
    button = button,
    sender = sender,
), GroupReplyAbleEvent