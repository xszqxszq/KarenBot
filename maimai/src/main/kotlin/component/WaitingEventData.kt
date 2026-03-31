package xyz.xszq.bot.component

import xyz.xszq.bot.event.MessageEvent

data class WaitingEventData(
    val event: MessageEvent,
    val expireAt: Long = System.currentTimeMillis() + 5 * 60 * 1000L
)
