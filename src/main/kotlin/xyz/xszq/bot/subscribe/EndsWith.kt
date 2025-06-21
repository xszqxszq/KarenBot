package xyz.xszq.bot.subscribe

import xyz.xszq.bot.event.Event
import xyz.xszq.bot.event.MessageEvent


/**
 * Subscribe of message ends with suffix.
 * @param parent Parent prefix of command.
 * @param forceParent Force check of parent prefix.
 * @param suffix The command's suffix.
 * @param handler Handle after matched.
 */
class EndsWith(
    parent: String? = null,
    forceParent: Boolean = false,
    suffix: String,
    handler: suspend MessageEvent.(String) -> Unit
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

    if (message.endsWith(suffix)) {
        val arg = message.substringBefore(suffix).trim()
        handler(event, arg)
    }
})