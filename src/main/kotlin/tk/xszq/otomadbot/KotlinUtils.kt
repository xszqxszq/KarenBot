@file:Suppress("unused", "JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")
package tk.xszq.otomadbot

import com.github.houbb.opencc4j.util.ZhConverterUtil
import com.soywiz.korau.format.readSoundInfo
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.std.toVfs
import com.soywiz.korio.net.MimeType
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import sun.security.action.GetPropertyAction.privilegedGetProperty
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.math.BigInteger
import java.nio.file.Path
import java.security.MessageDigest
import java.util.*
import javax.imageio.ImageIO
import kotlin.text.toCharArray

typealias Args = List<String>

const val availableUA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/85.0.4183.83 Safari/537.36"
val urlRegex = "^[A-Za-z0-9]*://.*$".toRegex()
val pass = {}
val tempDir = File(privilegedGetProperty("java.io.tmpdir").replace("\\", "/"))

suspend fun getAudioDuration(file: File): Double {
    return file.toVfs().readSoundInfo()?.duration?.seconds ?: 0.0
}
suspend fun VfsFile.getAudioDuration(): Double {
    return readSoundInfo()?.duration?.seconds ?: 0.0
}
fun File.getAudioDuration(): Double = runBlocking {
    toVfs().readSoundInfo()?.duration?.seconds ?: 0.0
}
fun String.escape(): String {
    return replace("\n", "\\n")
}
fun String.isEmptyChar(): Boolean {
    val text = trim()
    return text == "" || text == "\n" || text == "\t"
}
fun String.toArgsList(): List<String> = this.trim().split(" +".toRegex()).toMutableList().filter { isNotBlank() }
fun String.toArgsListByLn(): List<String> = this.trim().split("\n").toMutableList().filter { isNotBlank() }
fun String.toSimple(): String = ZhConverterUtil.toSimple(this)
fun String.toTraditional(): String = ZhConverterUtil.toTraditional(this)
fun String.trimLiteralTrident() = this.replace("    ", "")
fun String.limitDecimal(limit: Int = 4): String {
    if (toDoubleOrNull() == null)
        throw IllegalArgumentException("Only decimal String is allowed")
    var result = substringBefore('.') + '.'
    val afterPart = substringAfter('.')
    result += if (afterPart.length <= limit)
        afterPart + "0".repeat(4 - afterPart.length)
    else
        afterPart.substring(0, limit)
    return result
}
fun isLinux() = File("/bin/bash").exists()

fun ResponseBody.get(): String = string()
fun ResponseBody.getBytes(): ByteArray = bytes()
fun BufferedImage.toInputStream(): InputStream? {
    val stream = ByteArrayOutputStream()
    return try {
        ImageIO.write(this, "png", stream)
        ByteArrayInputStream(stream.toByteArray())
    } catch (e: Exception) {
        null
    }
}
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
fun getMIMEType(filename: Path): String {
    return MimeType.getByExtension(filename.extension).mime
}

fun String.md5(): String = BigInteger(1, MessageDigest.getInstance("MD5")
    .digest(toByteArray())).toString(16).padStart(32, '0')
fun File.md5(): String = BigInteger(1, MessageDigest.getInstance("MD5")
    .digest(readBytes())).toString(16).padStart(32, '0')
fun newTempFile(prefix: String = "", suffix: String = ""): File = tempDir.resolve(prefix +
        UUID.randomUUID().toString() + suffix)

const val DBC_SPACE = ' '
const val SBC_SPACE = 12288
const val DBC_CHAR_START = 33
const val DBC_CHAR_END = 126
const val CONVERT_STEP = 65248

fun String.toSBC(): String {
    val buf = StringBuilder(length)
    this.toCharArray().forEach {
        buf.append(
            when (it.code) {
                DBC_SPACE.code -> SBC_SPACE
                in DBC_CHAR_START..DBC_CHAR_END -> it + CONVERT_STEP
                else -> it
            }
        )
    }
    return buf.toString()
}
fun Char.isDBC() = this.code in DBC_SPACE.code..DBC_CHAR_END
enum class TextHAlign {
    LEFT,
    CENTER,
    RIGHT
}