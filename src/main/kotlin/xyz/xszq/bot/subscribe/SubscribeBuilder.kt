package xyz.xszq.bot.subscribe

import xyz.xszq.bot.event.ChannelEvent
import xyz.xszq.bot.event.InteractionEvent
import xyz.xszq.bot.event.MessageEvent

/**
 * route() 中构建订阅的 DSL Builder
 *
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
    /**
     * 命令订阅域
     *
     * 同一命令域可容纳多个插件的订阅，同一条消息只被一个订阅处理
     *
     * @param name 命令域名称
     * @param value 插件在命令域中的取值
     * @param defaultHandler 查询命令域默认取值
     * @param block 订阅路由
     */
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

    /**
     * 订阅与指定文本完全一致的消息
     *
     * @param text 命令文本
     * @param block 匹配后的处理逻辑
     */
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

    /**
     * 订阅指定前缀开头的消息
     *
     * @param text 命令前缀
     * @param block 匹配后的处理逻辑
     */
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

    /**
     * 订阅指定后缀结尾的消息
     *
     * @param text 命令后缀
     * @param block 匹配后的处理逻辑
     */
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

    /**
     * 订阅指定命令后缀结尾的消息
     *
     * 与 endsWith 的区别在于，匹配的是诸如“/parent arg1 toMatch arg2”的形式
     * 同时也可匹配“arg1 toMatch”、“toMatch arg2”、“toMatch”的情形
     *
     * @param text 命令后缀
     * @param block 匹配后的处理逻辑
     */
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

    /**
     * 无条件订阅全部消息事件
     *
     * @param block 处理逻辑
     */
    fun always(block: suspend MessageEvent.() -> Unit) {
        manager.subscribe(
            plugin,
            Always(block)
        )
    }

    /**
     * 订阅指定按钮 ID 的互动事件
     *
     * @param button 按钮 ID
     * @param block 匹配后的处理逻辑
     */
    fun button(button: String, block: suspend InteractionEvent.() -> Unit) {
        manager.subscribe(
            plugin,
            ButtonSubscribe(button, block)
        )
    }

    /**
     * 订阅指定名称的频道事件
     *
     * @param name 频道名
     * @param block 匹配后的处理逻辑
     */
    fun <T: Any> channel(name: String, block: suspend ChannelEvent<T>.(T) -> Unit) {
        manager.subscribe(
            plugin,
            Channel(name, block)
        )
    }
}