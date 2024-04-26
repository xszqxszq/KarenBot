package xyz.xszq.karenbot.kotlin

import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.std.tempVfs
import com.soywiz.korio.file.std.tmpdir
import com.soywiz.korio.net.MimeType
import io.ktor.util.*
import java.io.File
import java.nio.file.Path
import java.util.*

val tempDir = File(tmpdir)

fun newTempFile(prefix: String = "", suffix: String = ""): File = tempDir.resolve(prefix +
        UUID.randomUUID().toString() + suffix)
fun newTempVfsFile(prefix: String = "", suffix: String = ""): VfsFile = tempVfs[prefix +
        UUID.randomUUID().toString() + suffix]
fun getMIMEType(filename: Path): String {
    return MimeType.getByExtension(filename.extension).mime
}


fun getTempFile(prefix: String = "", suffix: String = "") = tempVfs[prefix + UUID.randomUUID().toString() + suffix]

suspend fun <R> VfsFile.useTempFile(block: suspend (VfsFile) -> R): R {
    return block(this).also { toFile().delete() }
}
suspend fun <R> useTempFile(prefix: String = "", suffix: String = "", block: suspend (VfsFile) -> R): R {
    val file = tempVfs[prefix + UUID.randomUUID().toString() + suffix]
    return block(file).also {
        file.delete()
    }
}

fun VfsFile.toFile() = File(absolutePath)