package xyz.xszq.bot.subscribe

import xyz.xszq.bot.event.Event
import xyz.xszq.bot.event.MessageEvent

/**
 * Subscribe of message without condition.
 * @param handler Handle after matched.
 */
class Always(
    handler: suspend MessageEvent.() -> Unit
): Subscribe<MessageEvent>(handler@{ event: Event ->
    if (event !is MessageEvent)
        return@handler
    handler(event)
})