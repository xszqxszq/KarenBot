package xyz.xszq.bot.event

import xyz.xszq.bot.Group

interface GroupEvent: Event {
    val group: Group
}