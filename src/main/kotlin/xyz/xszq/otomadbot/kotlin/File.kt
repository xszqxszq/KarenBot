package xyz.xszq.otomadbot.kotlin

import com.soywiz.korio.net.MimeType
import io.ktor.util.*
import java.io.File
import java.nio.file.Path
import java.util.*

val tempDir = File(System.getProperty("java.io.tmpdir").replace("\\", "/"))

fun newTempFile(prefix: String = "", suffix: String = ""): File = tempDir.resolve(prefix +
        UUID.randomUUID().toString() + suffix)
fun getMIMEType(filename: Path): String {
    return MimeType.getByExtension(filename.extension).mime
}