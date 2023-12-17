@file:Suppress("unused")

package xyz.xszq.nereides

import korlibs.image.awt.toBMP32
import korlibs.image.bitmap.Bitmap32
import korlibs.io.file.std.tmpdir
import kotlinx.coroutines.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import javax.imageio.ImageIO


val pass = {}

object JsonAsStringSerializer: JsonTransformingSerializer<String>(tSerializer = String.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        return JsonPrimitive(value = element.toString())
    }
}

fun parseDate(date: String): Long {
    return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse(date).time
}

fun LocalDateTime.toTimeStamp(timezone: String = "Asia/Shanghai"): Long {
    return atZone(ZoneId.of(timezone)).toInstant().toEpochMilli()
}

fun LocalDateTime.isSameDay(b: LocalDateTime): Boolean =
    year == b.year && month == b.month && dayOfMonth == b.dayOfMonth

val tempDir = File(tmpdir)

fun newTempFile(prefix: String = "", suffix: String = ""): File = tempDir.resolve(prefix +
        UUID.randomUUID().toString() + suffix)

suspend fun File.readAsImage(): Bitmap32 = withContext(Dispatchers.IO) {
    ImageIO.read(this@readAsImage).toBMP32()
}

val audioExts = listOf("mp3", "wav", "ogg", "m4a")

const val availableUA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/85.0.4183.83 Safari/537.36"



inline fun <T> Iterable<T>.sumOf(selector: (T) -> Float): Float {
    var sum: Float = 0.toFloat()
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

suspend inline fun <T, R> Iterable<T>.mapParallel(crossinline transform: suspend (T) -> R): List<R> {
    return runBlocking(Dispatchers.IO) {
        map {
            async {
                transform(it)
            }
        }.awaitAll()
    }
}
suspend inline fun IntArray.mapParallel(crossinline transform: suspend (Int) -> Int): IntArray {
    return runBlocking(Dispatchers.IO) {
        map {
            async {
                transform(it)
            }
        }.awaitAll().toTypedArray().toIntArray()
    }
}
suspend inline fun <T> Iterable<T>.forEachParallel(crossinline action: suspend (T) -> Unit) {
    return coroutineScope {
        map {
            async {
                action(it)
            }
        }.awaitAll()
    }
}