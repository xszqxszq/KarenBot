package xyz.xszq.nereides.message

import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.baseName
import xyz.xszq.bot.ffmpeg.toMp3BeforeSilk
import xyz.xszq.bot.ffmpeg.toSilk
import xyz.xszq.nereides.NetworkUtils
import java.io.File
import java.util.UUID

suspend fun File.toImage(): Image {
    return LocalImage(id = this.name, url = NetworkUtils.upload(this))
}
suspend fun VfsFile.toImage(): Image {
    return LocalImage(id = this.baseName, url = NetworkUtils.upload(File(this.absolutePath)))
}
suspend fun ByteArray.toImage(): Image {
    return LocalImage(id = UUID.randomUUID().toString(), url = NetworkUtils.uploadBinary(this))
}
suspend fun File.toVoice(): Voice {
    return LocalVoice(id = this.name, url = NetworkUtils.upload(toSilk()))
}
suspend fun VfsFile.toVoice(): Voice {
    return LocalVoice(id = this.baseName, url = NetworkUtils.upload(toSilk()))
}
fun String.toPlainText() = PlainText(this)
operator fun <A: Message, B: Message> A.plus(message: B): MessageChain {
    return MessageChain(listOf(this, message))
}