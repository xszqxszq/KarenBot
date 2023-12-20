@file:Suppress("MemberVisibilityCanBePrivate")

package xyz.xszq.nereides.event

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import xyz.xszq.config
import kotlin.coroutines.CoroutineContext

object GlobalEventChannel: CoroutineScope {
    override val coroutineContext: CoroutineContext = Job()
    val channel = MutableSharedFlow<Event>()
    suspend fun broadcast(event: Event) {
        channel.emit(event)
    }
    inline fun <reified T: Event> subscribe(crossinline block: suspend T.() -> Unit) {
        this.launch {
            channel.collect { event ->
                this.launch {
                if (event is T && (!config.sandbox ||
                            event !is PublicMessageEvent ||
                            event.contextId == "0532FA2F08EC28053EF1300409432E54")) {
//                    if (event is T) {
                        kotlin.runCatching {
                            block(event)
                        }.onFailure { e ->
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }
    fun subscribePublicMessage(block: suspend PublicMessageEvent.() -> Unit) = subscribe<PublicMessageEvent>(block)
    fun subscribePublicMessages(
        prefix: String = "",
        permName: String = "",
        forcePrefix: Boolean = false,
        block: PublicMessageSubscribeBuilder.() -> Unit
    ) {
        block(PublicMessageSubscribeBuilder(prefix, permName = permName, forcePrefix = forcePrefix))
    }
}