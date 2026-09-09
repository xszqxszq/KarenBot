package xyz.xszq.bot.subscribe

import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.util.toSimple

/**
 * 订阅指定前缀开头的消息
 *
 * @param parent 父级命令前缀
 * @param forceParent 是否一定要父级前缀
 * @param prefix 命令前缀
 * @param matchWord 是否防误触
 * @param matchHandler 匹配后的处理逻辑
 */
class StartsWith(
    parent: String? = null,
    forceParent: Boolean = false,
    private val prefix: String,
    private val matchWord: Boolean = false,
    private val matchHandler: suspend MessageEvent.(String) -> Unit
): TextSubscribe(parent, forceParent) {
    override val priority = 3
    override val length = prefix.length

    override fun matchesText(message: String) = when {
        !matchWord -> message.toSimple().startsWith(prefix.toSimple())
        else -> {
            val normalized = message.toSimple()
            val command = prefix.toSimple()
            normalized == command || normalized.startsWith("$command ")
        }
    }

    override suspend fun handleText(event: MessageEvent, message: String) {
        // 支持匹配繁体
        val arg = message.substring(prefix.length).trim()
        matchHandler(event, arg)
    }
}
