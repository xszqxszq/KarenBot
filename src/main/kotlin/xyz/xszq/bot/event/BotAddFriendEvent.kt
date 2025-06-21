package xyz.xszq.bot.event

import xyz.xszq.bot.Bot
import xyz.xszq.bot.User

class BotAddFriendEvent(
    override val bot: Bot,
    override val eventId: String,
    override val user: User
): UserEvent