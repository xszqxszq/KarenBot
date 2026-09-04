package xyz.xszq.bot.subscribe

import xyz.xszq.bot.event.ChannelEvent
import xyz.xszq.bot.event.Event

/**
 * 事件频道，用于插件之间互相通信
 *
 * @param name 频道名
 * @param handler 匹配后的处理逻辑
 */
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