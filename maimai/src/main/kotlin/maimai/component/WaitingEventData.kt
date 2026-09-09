package xyz.xszq.bot.maimai.component

import xyz.xszq.bot.event.MessageEvent

/**
 * 回调等待信息
 *
 * @property event 原始消息事件
 * @property replay 是否重放消息事件
 * @property expireAt 过期时间（毫秒）
 */
data class WaitingEventData(
    val event: MessageEvent,
    val replay: Boolean = false,
    val expireAt: Long = System.currentTimeMillis() + 20 * 60 * 1000L
)