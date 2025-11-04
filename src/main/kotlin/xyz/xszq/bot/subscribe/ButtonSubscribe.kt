package xyz.xszq.bot.subscribe

import xyz.xszq.bot.event.Event
import xyz.xszq.bot.event.InteractionEvent

class ButtonSubscribe(
    button: String,
    handler: suspend InteractionEvent.() -> Unit
): Subscribe<InteractionEvent>(handler@{ event: Event ->
    if (event !is InteractionEvent)
        return@handler

    if (event.button == button) {
        handler(event)
    }
})