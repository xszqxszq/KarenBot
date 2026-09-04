package xyz.xszq.bot.subscribe

import xyz.xszq.bot.event.Event
import xyz.xszq.bot.event.MessageEvent

/**
 * 订阅全部消息，无条件匹配
 *
 * @param handler 匹配后的处理逻辑
 */
class Always(
    handler: suspend MessageEvent.() -> Unit
): Subscribe<MessageEvent>(handler@{ event: Event ->
    if (event !is MessageEvent)
        return@handler
    handler(event)
})