package xyz.xszq.bot.subscribe

import xyz.xszq.bot.event.Event

/**
 * Subscribe to message.
 */
open class Subscribe<E: Event>(
    val handler: suspend (Event) -> Unit,
)