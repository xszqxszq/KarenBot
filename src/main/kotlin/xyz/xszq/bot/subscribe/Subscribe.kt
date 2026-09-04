package xyz.xszq.bot.subscribe

import xyz.xszq.bot.event.Event

/**
 * 事件订阅
 *
 * @param defaultHandler 默认事件处理
 */
open class Subscribe<E: Event>(
    private val defaultHandler: suspend (Event) -> Unit = {}
) {
    /**
     * 处理该事件
     */
    open suspend fun handle(event: Event) {
        defaultHandler(event)
    }

    /**
     * 判断事件是否命中订阅规则
     */
    open suspend fun matches(event: Event) = false

    /**
     * 事件处理
     */
    val handler: suspend (Event) -> Unit = { event ->
        handle(event)
    }
}