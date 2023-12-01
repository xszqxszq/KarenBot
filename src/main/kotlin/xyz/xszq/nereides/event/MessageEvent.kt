package xyz.xszq.nereides.event

import com.soywiz.korio.file.VfsFile
import xyz.xszq.nereides.message.Message
import xyz.xszq.nereides.message.MessageChain
import xyz.xszq.nereides.payload.message.MessageArk
import xyz.xszq.nereides.payload.response.PostGroupMessageResponse
import java.io.File

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