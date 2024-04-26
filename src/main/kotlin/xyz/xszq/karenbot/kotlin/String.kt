package xyz.xszq.karenbot.kotlin

import com.github.houbb.opencc4j.util.ZhConverterUtil
import com.github.promeg.pinyinhelper.Pinyin
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage

typealias Args = List<String>

val urlRegex = "^(http|https|ftp|magnet|ed2k)://.*$".toRegex()

fun String.toSimple(): String = ZhConverterUtil.toSimple(this)
fun String.isUrl() = urlRegex.matches(this)
fun String.trimLiteralTrident() = this.replace("    ", "")

val qrGenerateHint = buildMap {
    put(EncodeHintType.CHARACTER_SET, "utf-8")
    put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.Q)
    put(EncodeHintType.MARGIN, 2)
}
suspend fun String.generateQR(): BufferedImage? = withContext(Dispatchers.IO) {
    try {
        MultiFormatWriter().encode(this@generateQR, BarcodeFormat.QR_CODE,
            150, 150, qrGenerateHint).run {
            MatrixToImageWriter.toBufferedImage(this)
        }
    } catch (e: Exception) {
        null
    }
}
fun String.substringAfterPrefix(start: String): String = substring(start.length)
fun String.substringBeforeSuffix(suffix: String): String = substring(0, suffix.length)

fun String.toArgsList(): List<String> = this.trim().split(" +".toRegex()).toMutableList().filter { isNotBlank() }

fun String.toPinyinList() = Pinyin.toPinyin(this, ",").trim().split(",")

fun String.toPinyinAbbr(): String = toPinyinList().filter { it.isNotBlank() }.map { it.first() }.joinToString(separator="")

fun List<String>.subArgsList(): List<String> {
    if (size < 2)
        return listOf()
    return subList(1, size)
}
fun String.toArgsListByLn(): List<String> = this.trim().split("\r\n", "\r", "\n").toMutableList().filter {
    isNotBlank()
}
fun String.toArgsListByLnOrSpace(): List<String> = if ("\r" in this || "\n" in this)
    toArgsListByLn()
else
    toArgsList()