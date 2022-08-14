package xyz.xszq.otomadbot.kotlin

import com.github.houbb.opencc4j.util.ZhConverterUtil
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.awt.image.BufferedImage

typealias Args = List<String>

val urlRegex = "^(http|https|ftp|magnet|ed2k)://.*$".toRegex()

fun String.toSimple(): String = ZhConverterUtil.toSimple(this)
fun String.toArgsList(): List<String> = this.trim().split(" +".toRegex()).toMutableList().filter { isNotBlank() }
fun String.isUrl() = urlRegex.matches(this)
fun String.trimLiteralTrident() = this.replace("    ", "")


fun String.generateQR(): BufferedImage? {
    val link = this
    val hints = HashMap<EncodeHintType, Any>()
    hints[EncodeHintType.CHARACTER_SET] = "utf-8"
    hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.M
    hints[EncodeHintType.MARGIN] = 2
    return try {
        MatrixToImageWriter.toBufferedImage(MultiFormatWriter().encode(link, BarcodeFormat.QR_CODE, 300, 300, hints))
    } catch (e: Exception) {
        null
    }
}
fun String.substringAfterPrefix(start: String): String = substring(start.length)
fun String.substringBeforeSuffix(suffix: String): String = substring(0, suffix.length)