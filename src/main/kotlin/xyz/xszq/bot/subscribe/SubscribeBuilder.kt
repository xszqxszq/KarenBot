package xyz.xszq.bot.subscribe

import xyz.xszq.bot.event.InteractionEvent
import xyz.xszq.bot.event.MessageEvent

/**
 * Build subscribes in route().
 * @param plugin Plugin name.
 * @param prefix Plugin route prefix.
 * @param forcePrefix Should plugin force check its prefix.
 * @param manager Plugin manager.
 */
class SubscribeBuilder(
    val plugin: String,
    val prefix: String? = null,
    val forcePrefix: Boolean = false,
    private val manager: SubscribeManager
) {
    fun equalsTo(text: String, block: suspend MessageEvent.() -> Unit) {
        manager.subscribe(
            plugin,
            EqualsTo(prefix, forcePrefix, text, block)
        )
    }
    fun equalsTo(texts: Collection<String>, block: suspend MessageEvent.() -> Unit) = texts.forEach { text ->
        equalsTo(text, block)
    }
    fun startsWith(text: String, block: suspend MessageEvent.(String) -> Unit) {
        manager.subscribe(
            plugin,
            StartsWith(prefix, forcePrefix, text, block)
        )
    }
    fun startsWith(texts: Collection<String>, block: suspend MessageEvent.(String) -> Unit) = texts.forEach { text ->
        startsWith(text, block)
    }
    fun endsWith(text: String, block: suspend MessageEvent.(String) -> Unit) {
        manager.subscribe(
            plugin,
            EndsWith(prefix, forcePrefix, text, block)
        )
    }
    fun endsWith(texts: Collection<String>, block: suspend MessageEvent.(String) -> Unit) = texts.forEach { text ->
        endsWith(text, block)
    }
    fun commandEndsWith(text: String, block: suspend MessageEvent.(String) -> Unit) {
        manager.subscribe(
            plugin,
            CommandEndsWith(prefix, forcePrefix, text, block)
        )
    }
    fun commandEndsWith(texts: Collection<String>, block: suspend MessageEvent.(String) -> Unit) = texts.forEach { text ->
        commandEndsWith(text, block)
    }
    fun always(block: suspend MessageEvent.() -> Unit) {
        manager.subscribe(
            plugin,
            Always(block)
        )
    }

    fun button(button: String, embeddedMsgId: Boolean = false, block: suspend InteractionEvent.() -> Unit) {
        manager.subscribe(
            plugin,
            ButtonSubscribe(button, embeddedMsgId, block)
        )
    }
}