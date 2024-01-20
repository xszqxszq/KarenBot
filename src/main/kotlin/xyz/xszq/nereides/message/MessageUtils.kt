package xyz.xszq.nereides.message

import korlibs.io.file.VfsFile
import korlibs.io.file.baseName
import korlibs.io.file.std.toVfs
import korlibs.io.util.UUID
import java.io.File

fun File.toImage(): Image {
    return LocalImage(toVfs(), id = this.name)
}
fun VfsFile.toImage(): Image {
    return LocalImage(this, id = this.baseName)
}
fun ByteArray.toImage(): Image {
    return LocalImage(null, id = UUID.randomUUID().toString()).also {
        it.bytes = this
    }
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