package xyz.xszq.bot.subscribe

import xyz.xszq.bot.event.Event
import xyz.xszq.bot.event.InteractionEvent

/**
 * 订阅指定 ID 按钮的互动事件
 *
 * @param button 按钮 ID
 * @param handler 匹配后的处理逻辑
 */
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