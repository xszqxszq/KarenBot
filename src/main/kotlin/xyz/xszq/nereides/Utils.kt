@file:Suppress("unused")

package xyz.xszq.nereides

import com.soywiz.korim.awt.toBMP32
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korio.file.std.tmpdir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

fun Double.radTo180Deg(): Double {
    if (this in -Math.PI .. Math.PI)
        return this
    var result = this
    return if (this > 0) {
        while (result > Math.PI)
            result -= Math.PI * 2
        result
    } else {
        while (result < -Math.PI)
            result += Math.PI * 2
        result
    }
}