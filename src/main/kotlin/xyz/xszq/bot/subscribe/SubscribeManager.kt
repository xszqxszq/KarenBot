package xyz.xszq.bot.subscribe

import kotlinx.coroutines.*
import xyz.xszq.bot.event.Event
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.util.Metrics
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.CoroutineContext

/**
 * 管理插件的订阅
 *
 * @param dispatcher 订阅处理所在的调度器
 */
class SubscribeManager(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
): CoroutineScope {
    override val coroutineContext: CoroutineContext = Job()
    private val plugins = ConcurrentHashMap<String, CopyOnWriteArrayList<Subscribe<Event>>>()
    private val temp = ConcurrentHashMap<String, Subscribe<Event>>()
    private val orderId = AtomicLong(0)

    private data class TextHandler(
        val plugin: String,
        val subscribe: TextSubscribe
    )

    /**
     * 添加一个订阅
     *
     * @param plugin 插件名
     * @param subscribe 订阅
     * @param domain 所属命令域
     * @param value 订阅在命令域中的取值
     * @param defaultHandler 命令域默认取值查询函数
     */
    fun <E: Event> subscribe(
        plugin: String,
        subscribe: Subscribe<E>,
        domain: String? = null,
        value: String? = null,
        defaultHandler: (suspend MessageEvent.() -> String?)? = null
    ) {
        if (!plugins.containsKey(plugin)) {
            plugins[plugin] = CopyOnWriteArrayList()
        }
        @Suppress("UNCHECKED_CAST")
        val eventSubscribe = subscribe as Subscribe<Event>
        (eventSubscribe as? TextSubscribe)?.let {
            it.domain = domain
            it.value = value
            it.defaultHandler = defaultHandler
            it.order = orderId.getAndIncrement()
        }
        plugins[plugin]?.add(eventSubscribe)
    }

    // TODO: 支持全部事件

    /**
     * 添加临时的无条件订阅
     *
     * @param name 名称
     * @param handler 处理逻辑
     */
    fun always(name: String, handler: suspend MessageEvent.() -> Unit) {
        @Suppress("UNCHECKED_CAST")
        temp[name] = Always(handler) as Subscribe<Event>
    }

    /**
     * 移除指定名称的临时订阅
     *
     * @param name 名称
     */
    fun stop(name: String) {
        temp.remove(name)
    }

    /**
     * 移除插件的全部订阅
     *
     * @param plugin 插件名
     */
    fun unsubscribe(plugin: String) {
        plugins.remove(plugin)
    }

    /**
     * 触发事件的订阅处理
     * @param event 待处理事件
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun <E: Event> handle(event: E) = Metrics.time("karenbot.event.processing") {
        supervisorScope {
            temp.values.forEach { subscribe ->
                launchHandle("temporary") { subscribe.handle(event) }
            }

            val prefix = explicitPrefix(event)
            val groups = linkedMapOf<String, MutableList<TextHandler>>()
            plugins.forEach { (plugin, subscribes) ->
                subscribes.forEach { subscribe ->
                    val textSubscribe = subscribe as? TextSubscribe
                    if (textSubscribe?.domain == null) {
                        launchHandle(plugin) { subscribe.handle(event) }
                        return@forEach
                    }
                    if (prefix != null && textSubscribe.parent != prefix)
                        return@forEach
                    if (textSubscribe.matches(event)) {
                        groups.getOrPut(textSubscribe.domain!!) { mutableListOf() }
                            .add(TextHandler(plugin, textSubscribe))
                    }
                }
            }

            groups.values.forEach { handlers ->
                val plugin = handlers.map { it.plugin }.distinct().singleOrNull() ?: "multi"
                launchHandle(plugin) { runText(event, plugin, handlers.map { it.subscribe }) }
            }
        }
    }

    private suspend fun runText(
        event: Event,
        plugin: String,
        list: List<TextSubscribe>
    ) {
        orderText(event, list).forEach { subscribe ->
            try {
                subscribe.handle(event)
                Metrics.count(
                    "karenbot.plugin.match",
                    "plugin" to plugin,
                    "outcome" to "matched"
                )
                return
            } catch (_: CommandNotMatchedException) {
                Metrics.count(
                    "karenbot.plugin.match",
                    "plugin" to plugin,
                    "outcome" to "unmatched"
                )
            }
        }
    }

    private suspend fun orderText(event: Event, list: List<TextSubscribe>): List<TextSubscribe> {
        if (list.isEmpty())
            return emptyList()

        val defaults = if (event is MessageEvent)
            list.mapNotNull { it.defaultHandler?.invoke(event)?.trim()?.ifBlank { null } }.distinct()
        else emptyList()
        val game = defaults.singleOrNull()

        return list.sortedWith(
            compareByDescending<TextSubscribe> { it.priority }
                .thenByDescending { it.length }
                .thenByDescending { game != null && it.value == game }
                .thenBy { it.order }
        )
    }

    private fun explicitPrefix(event: Event): String? {
        if (event !is MessageEvent)
            return null
        val now = event.text.trim()
        if (!now.startsWith("/"))
            return null
        return plugins.values.asSequence()
            .flatMap { it.asSequence() }
            .mapNotNull { (it as? TextSubscribe)?.parent }
            .distinct()
            .firstOrNull { now.startsWith(it) }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun CoroutineScope.launchHandle(
        plugin: String,
        block: suspend () -> Unit
    ) {
        launch(dispatcher) {
            runCatching {
                Metrics.time(
                    "karenbot.plugin.handler",
                    "plugin" to plugin
                ) {
                    block()
                }
            }.onFailure { e ->
                if (e !is CancellationException)
                    e.printStackTrace()
            }
        }
    }
}