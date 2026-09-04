package xyz.xszq.bot.subscribe

import xyz.xszq.bot.event.Event
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.util.normalizeMessage

/**
 * 文本消息订阅
 *
 * @param parent 父级命令前缀
 * @param forceParent 是否必须带父级前缀
 */
abstract class TextSubscribe(
    val parent: String? = null,
    private val forceParent: Boolean = false
): Subscribe<MessageEvent>() {
    /**
     * 所属命令域
     */
    var domain: String? = null

    /**
     * 插件在命令域中的取值
     */
    var value: String? = null

    /**
     * 默认处理函数
     */
    var defaultHandler: (suspend MessageEvent.() -> String?)? = null

    /**
     * 注册序号，数值较小的订阅优先执行
     */
    var order = 0L

    abstract val priority: Int
    abstract val length: Int

    final override suspend fun handle(event: Event) {
        val (messageEvent, message) = normalizeMessage(event, parent, forceParent) ?: return
        if (!matchesText(message))
            return
        handleText(messageEvent, message)
    }

    final override suspend fun matches(event: Event): Boolean {
        val (_, message) = normalizeMessage(event, parent, forceParent) ?: return false
        return matchesText(message)
    }

    protected abstract fun matchesText(message: String): Boolean
    protected abstract suspend fun handleText(event: MessageEvent, message: String)
}