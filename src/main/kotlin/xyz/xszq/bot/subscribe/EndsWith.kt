package xyz.xszq.bot.subscribe

import xyz.xszq.bot.event.MessageEvent

/**
 * 订阅指定后缀结尾的消息
 *
 * @param parent 父级命令前缀
 * @param forceParent 是否一定要父级前缀
 * @param suffix 命令后缀
 * @param matchHandler 匹配后的处理逻辑
 */
class EndsWith(
    parent: String? = null,
    forceParent: Boolean = false,
    private val suffix: String,
    private val matchHandler: suspend MessageEvent.(String) -> Unit
): TextSubscribe(parent, forceParent) {
    override val priority = 1
    override val length = suffix.length

    override fun matchesText(message: String) = message.endsWith(suffix)

    override suspend fun handleText(event: MessageEvent, message: String) {
        val arg = message.substringBefore(suffix).trim()
        matchHandler(event, arg)
    }
}