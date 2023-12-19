package xyz.xszq.nereides.event

import kotlinx.coroutines.Deferred
import xyz.xszq.nereides.message.Message
import xyz.xszq.nereides.message.MessageChain

interface MessageEvent: Event {
    val msgId: String
    val subjectId: String
    val message: MessageChain
    val contentString: String
    val timestamp: Long
    suspend fun reply(content: String): Boolean
    suspend fun reply(content: Message): Boolean
    suspend fun reply(content: MessageChain): Boolean
}