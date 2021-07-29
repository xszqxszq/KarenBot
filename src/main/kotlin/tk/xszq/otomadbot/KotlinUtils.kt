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
import org.jetbrains.exposed.sql.*
import sun.security.action.GetPropertyAction.privilegedGetProperty
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.nio.file.Path
import java.util.*
import javax.imageio.ImageIO

const val availableUA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/85.0.4183.83 Safari/537.36"
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
fun String.toSimple(): String = ZhConverterUtil.toSimple(this)
fun String.trimLiteralTrident() = this.replace("    ", "")
fun isLinux() = File("/bin/bash").exists()

fun ResponseBody.get(): String = string()
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
class InStr(expr1: String, expr2: Expression<*>): ComparisonOp(
    CustomStringFunction("INSTR", stringParam(expr1), expr2), intParam(0), "<>")

class RegexpOpCol<T : String?>(
    /** Returns the expression being checked. */
    private val expr1: Expression<T>,
    /** Returns the regular expression [expr1] is checked against. */
    private val expr2: String
) : Op<Boolean>(), ComplexExpression {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder {
            append(expr1, " REGEXP `$expr2`")
        }
    }
}

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
fun String.lowercase(): String = (this as java.lang.String).toLowerCase(Locale.ROOT)
fun newTempFile(prefix: String = "", suffix: String = ""): File = tempDir.resolve(prefix +
        UUID.randomUUID().toString() + suffix)