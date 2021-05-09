@file:Suppress("unused")
package tk.xszq.otomadbot

import com.github.houbb.opencc4j.util.ZhConverterUtil
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.net.MimeType
import com.soywiz.korio.util.UUID
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import sun.security.action.GetPropertyAction
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.math.BigInteger
import java.nio.file.Path
import java.security.AccessController
import java.security.MessageDigest
import javax.imageio.ImageIO

val pass = {}

val tempDir: String = privilegedGetProperty("java.io.tmpdir").replace("\\", "/") + "/"

fun String.escape(): String {
    return replace("\n", "\\n")
}
/**
 * Convert String to MD5.
 */
fun String.md5(): String {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(toByteArray())).toString(16).padStart(32, '0')
}
fun String.isEmptyChar(): Boolean {
    val text = trim()
    return text == "" || text == "\n" || text == "\t"
}
fun String.toArgsList(): List<String> = this.trim().split(" +".toRegex()).toMutableList().filter { isNotBlank() }
fun String.toSimple(): String = ZhConverterUtil.toSimple(this)
fun String.concatDot(another: String): String = if (isBlank()) another else "$this.$another"
fun String.trimLiteralTrident() = this.replace("    ", "")

fun getMIMEType(filename: Path): String {
    return MimeType.getByExtension(filename.extension).mime
}

fun HttpResponse.isSuccessful() = this.status == HttpStatusCode.OK

fun privilegedGetProperty(theProp: String): String {
    return if (System.getSecurityManager() == null) {
        System.getProperty(theProp)
    } else {
        AccessController.doPrivileged(
            GetPropertyAction(theProp)
        )
    }
}

suspend fun newTempFile(prefix: String = "", suffix: String = ""): File = withContext(Dispatchers.IO) {
    File((tempDir + "/" + prefix + UUID.randomUUID().toString() + suffix).replace("//", "/"))
}
fun newTempFileBlocking(prefix: String = "", suffix: String = ""): File =
    File((tempDir + "/" + prefix + UUID.randomUUID().toString() + suffix).replace("//", "/"))

fun VfsFile.toFile() = File(absolutePath)

@Suppress("UNCHECKED_CAST")
inline fun <R, reified U> Any.forceGetField(fieldName: String): R {
    val field = U::class.java.getDeclaredField(fieldName)
    field.isAccessible = true
    return field.get(this) as R
}
@Suppress("UNCHECKED_CAST")
inline fun <R, reified U> Any.forceSetField(fieldName: String, value: R) {
    val field = U::class.java.getDeclaredField(fieldName)
    field.isAccessible = true
    field.set(this, value)
}

fun isLinux() = File("/bin/bash").exists()
fun BufferedImage.toByteArrayBlocking(): ByteArray {
    val bos = ByteArrayOutputStream()
    ImageIO.write(this, "png", bos)
    return bos.toByteArray()
}
suspend fun BufferedImage.toByteArray(): ByteArray = withContext(Dispatchers.IO) {
    toByteArrayBlocking()
}
fun Collection<String>.matchString(str: String): String? {
    var result: String? = null
    forEach {
        if (it in str) {
            result = it
            return@forEach
        }
    }
    return result
}