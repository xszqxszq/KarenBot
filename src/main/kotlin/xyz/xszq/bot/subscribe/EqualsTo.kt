package xyz.xszq.bot.subscribe

import xyz.xszq.bot.event.MessageEvent

/**
 * 订阅与指定文本完全一致的消息
 *
 * @param parent 父级命令前缀
 * @param forceParent 是否必须带父级前缀
 * @param text 命令文本
 * @param matchHandler 匹配后的处理逻辑
 */
class EqualsTo(
    parent: String? = null,
    forceParent: Boolean = false,
    private val text: String,
    private val matchHandler: suspend MessageEvent.() -> Unit
): TextSubscribe(parent, forceParent) {
    override val priority = 4
    override val length = text.length

    override fun matchesText(message: String) = message == text

    override suspend fun handleText(event: MessageEvent, message: String) {
        matchHandler(event)
    }
}