package xyz.xszq.nereides.message

import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.baseName
import com.soywiz.korio.file.std.toVfs
import xyz.xszq.bot.ffmpeg.toMp3BeforeSilk
import xyz.xszq.bot.ffmpeg.toSilk
import xyz.xszq.nereides.NetworkUtils
import xyz.xszq.nereides.newTempFile
import java.io.File
import java.util.UUID

suspend fun File.toImage(): Image {
    return LocalImage(toVfs(), id = this.name)
}
suspend fun VfsFile.toImage(): Image {
    return LocalImage(this, id = this.baseName)
}
suspend fun ByteArray.toImage(): Image {
    val file = newTempFile().toVfs()
    file.writeBytes(this)
    return LocalImage(file, id = file.baseName)
}
suspend fun File.toVoice(): Voice {
    return LocalVoice(toVfs(), id = this.name)
}
suspend fun VfsFile.toVoice(): Voice {
    return LocalVoice(this, id = this.baseName)
}
fun String.toPlainText() = PlainText(this)
operator fun <A: Message, B: Message> A.plus(message: B): MessageChain {
    return MessageChain(listOf(this, message))
}