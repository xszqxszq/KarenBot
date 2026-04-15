package xyz.xszq.bot.event

import korlibs.io.util.UUID
import xyz.xszq.bot.Bot

class ChannelEvent<T: Any>(
    override val bot: Bot,
    override val eventId: String = UUID.randomUUID().toString(),
    val channelName: String,
    val data: T
): Event