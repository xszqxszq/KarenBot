package xyz.xszq.bot.subscribe

import xyz.xszq.bot.event.Event
import xyz.xszq.bot.event.InteractionEvent

class ButtonSubscribe(
    button: String,
    embeddedMsgId: Boolean = false,
    handler: suspend InteractionEvent.() -> Unit
): Subscribe<InteractionEvent>(handler@{ event: Event ->
    if (event !is InteractionEvent)
        return@handler

    if (event.button == button) {
        if (embeddedMsgId) {
            val content = event.data.split(":", limit=3)
            event.id = content[0]
            event.seq = content[1].toInt()
            event.data = content[2]
        }
        handler(event)
    }
})