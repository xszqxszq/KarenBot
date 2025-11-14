package xyz.xszq.bot.subscribe

import kotlinx.coroutines.*
import xyz.xszq.bot.event.Event
import xyz.xszq.bot.event.MessageEvent
import kotlin.coroutines.CoroutineContext

/**
 * Manage subscribe of plugins.
 */
class SubscribeManager: CoroutineScope {
    override val coroutineContext: CoroutineContext = Job()
    private val plugins = mutableMapOf<String, MutableList<Subscribe<Event>>>()
    private val temp = mutableMapOf<String, Subscribe<Event>>()

    /**
     * Add a Subscribe from the plugin.
     * @param plugin Plugin's name.
     * @param subscribe Subscribe.
     */
    fun <E: Event> subscribe(plugin: String, subscribe: Subscribe<E>) {
        if (!plugins.containsKey(plugin)) {
            plugins[plugin] = mutableListOf()
        }
        @Suppress("UNCHECKED_CAST")
        plugins[plugin]?.add(subscribe as Subscribe<Event>)
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
     * Launch the handler of events under GlobalScope.
     * @param event Event to handle.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun <E: Event> handle(event: E) = coroutineScope {
        temp.values.forEach { subscribe ->
            launch(Dispatchers.IO) {
                subscribe.handler(event)
            }
        }
        plugins.forEach { plugin, subscribes ->
            subscribes.forEach { subscribe ->
                launch(Dispatchers.IO) {
                    subscribe.handler(event)
                }
            }
        }
    }
}