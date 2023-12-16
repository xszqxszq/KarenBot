package xyz.xszq.nereides.message

import korlibs.io.file.VfsFile
import korlibs.io.file.baseName
import korlibs.io.file.std.toVfs
import xyz.xszq.nereides.newTempFile
import java.io.File

fun File.toImage(): Image {
    return LocalImage(toVfs(), id = this.name)
}
fun VfsFile.toImage(): Image {
    return LocalImage(this, id = this.baseName)
}
suspend fun ByteArray.toImage(): Image {
    val file = newTempFile().toVfs()
    file.writeBytes(this)
    return LocalImage(file, id = file.baseName)
}
fun File.toVoice(): Voice {
    return LocalVoice(toVfs(), id = this.name)
}
fun VfsFile.toVoice(): Voice {
    return LocalVoice(this, id = this.baseName)
}
fun String.toPlainText() = PlainText(this)
operator fun <A: Message, B: Message> A.plus(message: B): MessageChain {
    return MessageChain(listOf(this, message))
}