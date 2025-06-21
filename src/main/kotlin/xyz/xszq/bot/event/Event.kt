package xyz.xszq.bot.event

import xyz.xszq.bot.Bot

interface Event {
    val bot: Bot
    val eventId: String
}