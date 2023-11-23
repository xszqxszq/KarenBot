package xyz.xszq.nereides.event

import com.soywiz.korio.file.VfsFile
import xyz.xszq.nereides.payload.message.MessageArk
import java.io.File

interface MessageEvent: Event {
    val msgId: String
    val subjectId: String
    val content: String
    val timestamp: Long
    suspend fun reply(content: String)
    suspend fun sendImage(file: File)
    suspend fun sendImage(file: VfsFile)
    suspend fun sendImage(binary: ByteArray)
}