@file:Suppress("MemberVisibilityCanBePrivate")

package xyz.xszq.nereides.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

object GlobalEventChannel: CoroutineScope {
    override val coroutineContext: CoroutineContext = Job()
    val channel = MutableSharedFlow<Event>()
    suspend fun broadcast(event: Event) {
        channel.emit(event)
    }
    inline fun <reified T: Event> subscribe(crossinline block: suspend T.() -> Unit) {
        launch {
            channel.collect { event ->
                if (event is T) {
                    kotlin.runCatching {
                        block(event)
                    }.onFailure { e ->
                        e.printStackTrace()
                    }
                }
            }
        }
    }
    fun subscribeMessage(block: suspend MessageEvent.() -> Unit) = subscribe<MessageEvent>(block)
    fun subscribeMessages(prefix: String = "", block: MessageSubscribeBuilder.() -> Unit) {
        block(MessageSubscribeBuilder(prefix))
    }
}