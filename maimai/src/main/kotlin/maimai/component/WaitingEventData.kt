package xyz.xszq.bot.maimai.component

import xyz.xszq.bot.event.MessageEvent

data class WaitingEventData(
    val event: MessageEvent,
    val replay: Boolean = false,
    val expireAt: Long = System.currentTimeMillis() + 20 * 60 * 1000L
)