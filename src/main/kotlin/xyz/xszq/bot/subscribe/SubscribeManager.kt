package xyz.xszq.bot.subscribe

import kotlinx.coroutines.*
import xyz.xszq.bot.event.Event
import xyz.xszq.bot.event.MessageEvent
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.CoroutineContext

/**
 * Manage subscribe of plugins.
 */
class SubscribeManager(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
): CoroutineScope {
    override val coroutineContext: CoroutineContext = Job()
    private val plugins = ConcurrentHashMap<String, CopyOnWriteArrayList<Subscribe<Event>>>()
    private val temp = ConcurrentHashMap<String, Subscribe<Event>>()
    private val orderId = AtomicLong(0)

    /**
     * Add a Subscribe from the plugin.
     * @param plugin Plugin's name.
     * @param subscribe Subscribe.
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

    // TODO: Support all events
    /**
     * Add a Temporary always subscribe from the plugin.
     * @param name Name.
     * @param subscribe Subscribe.
     */
    fun always(name: String, handler: suspend MessageEvent.() -> Unit) {
        @Suppress("UNCHECKED_CAST")
        temp[name] = Always(handler) as Subscribe<Event>
    }

    /**
     * Remove named subscribe.
     * @param name Name.
     */
    fun stop(name: String) {
        temp.remove(name)
    }

    /**
     * Remove all subscribes from the plugin.
     * @param plugin Plugin's name.
     */
    fun unsubscribe(plugin: String) {
        plugins.remove(plugin)
    }

    /**
     * Launch the handler of events.
     * @param event Event to handle.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun <E: Event> handle(event: E) = supervisorScope {
        temp.values.forEach { subscribe ->
            launchHandle { subscribe.handle(event) }
        }

        val prefix = explicitPrefix(event)
        val groups = linkedMapOf<String, MutableList<TextSubscribe>>()
        plugins.values.forEach { subscribes ->
            subscribes.forEach { subscribe ->
                val textSubscribe = subscribe as? TextSubscribe
                if (textSubscribe?.domain == null) {
                    launchHandle { subscribe.handle(event) }
                    return@forEach
                }
                if (prefix != null && textSubscribe.parent != prefix)
                    return@forEach
                if (textSubscribe.matches(event)) {
                    groups.getOrPut(textSubscribe.domain!!) { mutableListOf() }.add(textSubscribe)
                }
            }
        }

        groups.values.forEach { list ->
            launchHandle { runText(event, list) }
        }
    }

    private suspend fun runText(event: Event, list: List<TextSubscribe>) {
        orderText(event, list).forEach { subscribe ->
            try {
                subscribe.handle(event)
                return
            } catch (_: CommandNotMatchedException) {
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
    private fun CoroutineScope.launchHandle(block: suspend () -> Unit) {
        launch(dispatcher) {
            runCatching {
                block()
            }.onFailure { e ->
                e.printStackTrace()
            }
        }
    }
}