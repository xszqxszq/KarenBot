package xyz.xszq.bot.subscribe

import xyz.xszq.bot.event.Event
import xyz.xszq.bot.event.MessageEvent

/**
 * Subscribe of message equals to string.
 * @param parent Parent prefix of command.
 * @param forceParent Force check of parent prefix.
 * @param text The command.
 * @param handler Handle after matched.
 */
class EqualsTo(
    parent: String? = null,
    forceParent: Boolean = false,
    text: String,
    handler: suspend MessageEvent.() -> Unit
): Subscribe<MessageEvent>(handler@{ event: Event ->
    if (event !is MessageEvent)
        return@handler

    var message = event.text.trim()
    parent?.let {
        if (message.startsWith(it))
            message = message.substringAfter(it).trim()
        else if (forceParent) // If force and parent prefix not found
            return@handler
    }
    message = message.removePrefix("/").trim()

    if (message == text) {
        handler(event)
    }
})