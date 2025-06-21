package xyz.xszq.bot.subscribe

import xyz.xszq.bot.event.Event
import xyz.xszq.bot.event.MessageEvent

/**
 * Subscribe of message starts with prefix.
 * @param parent Parent prefix of command.
 * @param forceParent Force check of parent prefix.
 * @param prefix The command's prefix.
 * @param handler Handle after matched.
 */
class StartsWith(
    parent: String? = null,
    forceParent: Boolean = false,
    prefix: String,
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

    if (message.startsWith(prefix)) {
        val arg = message.substringAfter(prefix).trim()
        handler(event, arg)
    }
})