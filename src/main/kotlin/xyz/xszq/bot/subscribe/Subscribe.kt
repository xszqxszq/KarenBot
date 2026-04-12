package xyz.xszq.bot.subscribe

import xyz.xszq.bot.event.Event

/**
 * Subscribe to message.
 */
open class Subscribe<E: Event>(
    private val defaultHandler: suspend (Event) -> Unit = {}
) {
    open suspend fun handle(event: Event) {
        defaultHandler(event)
    }

    open suspend fun matches(event: Event) = false

    val handler: suspend (Event) -> Unit = { event ->
        handle(event)
    }
}