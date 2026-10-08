package xyz.xszq.bot.subscribe

import xyz.xszq.bot.event.MessageEvent

/**
 * 订阅全部消息，无条件匹配
 *
 * 指定命令域后与同域订阅互斥，因优先级最低只在其他订阅不处理时接管
 *
 * @param block 匹配后的处理逻辑
 */
class Always(
    private val block: suspend MessageEvent.() -> Unit
): TextSubscribe() {
    override val priority = 0
    override val length = 0

    override fun matchesText(message: String) = true

    override suspend fun handleText(event: MessageEvent, message: String) {
        block(event)
    }
}