package xyz.xszq.bot.subscribe

import xyz.xszq.bot.event.ChannelEvent
import xyz.xszq.bot.event.InteractionEvent
import xyz.xszq.bot.event.MessageEvent

/**
 * route() 中构建订阅的 DSL Builder
 * @param plugin 插件名
 * @param prefix 父级前缀
 * @param forcePrefix 是否一定要父级前缀
 * @param manager 订阅管理器
 */
class SubscribeBuilder(
    val plugin: String,
    val prefix: String? = null,
    val forcePrefix: Boolean = false,
    private val manager: SubscribeManager,
    private val domain: String? = null,
    private val value: String? = null,
    private val defaultHandler: suspend MessageEvent.() -> String? = { null }
) {
    suspend fun domain(
        name: String,
        value: String,
        defaultHandler: suspend MessageEvent.() -> String? = { null },
        block: suspend SubscribeBuilder.() -> Unit
    ) {
        block(SubscribeBuilder(
            plugin = plugin,
            prefix = prefix,
            forcePrefix = forcePrefix,
            manager = manager,
            domain = name,
            value = value,
            defaultHandler = defaultHandler
        ))
    }

    fun equalsTo(text: String, block: suspend MessageEvent.() -> Unit) {
        manager.subscribe(
            plugin,
            EqualsTo(prefix, forcePrefix, text, block),
            domain,
            value,
            defaultHandler
        )
    }
    fun equalsTo(texts: Collection<String>, block: suspend MessageEvent.() -> Unit) = texts.forEach { text ->
        equalsTo(text, block)
    }
    fun startsWith(text: String, block: suspend MessageEvent.(String) -> Unit) {
        manager.subscribe(
            plugin,
            StartsWith(prefix, forcePrefix, text, block),
            domain,
            value,
            defaultHandler
        )
    }
    fun startsWith(texts: Collection<String>, block: suspend MessageEvent.(String) -> Unit) = texts.forEach { text ->
        startsWith(text, block)
    }
    fun endsWith(text: String, block: suspend MessageEvent.(String) -> Unit) {
        manager.subscribe(
            plugin,
            EndsWith(prefix, forcePrefix, text, block),
            domain,
            value,
            defaultHandler
        )
    }
    fun endsWith(texts: Collection<String>, block: suspend MessageEvent.(String) -> Unit) = texts.forEach { text ->
        endsWith(text, block)
    }
    fun commandEndsWith(text: String, block: suspend MessageEvent.(Pair<String, String?>) -> Unit) {
        manager.subscribe(
            plugin,
            CommandEndsWith(prefix, forcePrefix, text, block),
            domain,
            value,
            defaultHandler
        )
    }
    fun commandEndsWith(texts: Collection<String>, block: suspend MessageEvent.(Pair<String, String?>) -> Unit) = texts.forEach { text ->
        commandEndsWith(text, block)
    }
    fun always(block: suspend MessageEvent.() -> Unit) {
        manager.subscribe(
            plugin,
            Always(block)
        )
    }

    fun button(button: String, block: suspend InteractionEvent.() -> Unit) {
        manager.subscribe(
            plugin,
            ButtonSubscribe(button, block)
        )
    }
    fun <T: Any> channel(name: String, block: suspend ChannelEvent<T>.(T) -> Unit) {
        manager.subscribe(
            plugin,
            Channel(name, block)
        )
    }
}