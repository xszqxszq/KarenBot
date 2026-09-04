package xyz.xszq.bot.subscribe

import xyz.xszq.bot.event.MessageEvent

/**
 * 订阅指定前缀开头的消息
 * @param parent 父级命令前缀
 * @param forceParent 是否一定要父级前缀
 * @param prefix 命令前缀
 * @param matchHandler 匹配后的处理逻辑
 */
class StartsWith(
    parent: String? = null,
    forceParent: Boolean = false,
    private val prefix: String,
    private val matchHandler: suspend MessageEvent.(String) -> Unit
): TextSubscribe(parent, forceParent) {
    override val priority = 3
    override val length = prefix.length

    override fun matchesText(message: String) = message.startsWith(prefix)

    override suspend fun handleText(event: MessageEvent, message: String) {
        val arg = message.substringAfter(prefix).trim()
        matchHandler(event, arg)
    }
}