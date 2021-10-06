package tk.xszq.otomadbot.image

import com.google.zxing.*
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.client.j2se.MatrixToImageWriter.toBufferedImage
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileInputStream
import javax.imageio.ImageIO
import kotlin.collections.set

fun File.decodeQR(): String {
    val binarizer = HybridBinarizer(BufferedImageLuminanceSource(ImageIO.read(FileInputStream(this))))
    val decodeHints = HashMap<DecodeHintType, Any?>()
    decodeHints[DecodeHintType.CHARACTER_SET] = "UTF-8"
    val result = MultiFormatReader().decode(BinaryBitmap(binarizer), decodeHints)
    return result.text
}

fun String.generateQR(): BufferedImage? {
    val link = this
    val hints = HashMap<EncodeHintType, Any>()
    hints[EncodeHintType.CHARACTER_SET] = "utf-8"
    hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.M
    hints[EncodeHintType.MARGIN] = 2
    return try {
        toBufferedImage(MultiFormatWriter().encode(link, BarcodeFormat.QR_CODE, 300, 300, hints))
    } catch (e: Exception) {
        null
    }
}