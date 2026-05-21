package xyz.xszq.bot.subscribe

import xyz.xszq.bot.event.ChannelEvent
import xyz.xszq.bot.event.Event

class Channel<T: Any>(
    val name: String,
    handler: suspend ChannelEvent<T>.(T) -> Unit
): Subscribe<ChannelEvent<T>>(handler@{ event: Event ->
    if (event !is ChannelEvent<*> || name != event.channelName)
        return@handler

    @Suppress("UNCHECKED_CAST")
    val typedEvent = event as ChannelEvent<T>
    handler(typedEvent, typedEvent.data)
})