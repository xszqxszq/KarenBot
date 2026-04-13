package xyz.xszq.bot.subscribe

import xyz.xszq.bot.event.Event
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.normalizeMessage

abstract class TextSubscribe(
    val parent: String? = null,
    private val forceParent: Boolean = false
): Subscribe<MessageEvent>() {
    var domain: String? = null
    var value: String? = null
    var defaultHandler: (suspend MessageEvent.() -> String?)? = null
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