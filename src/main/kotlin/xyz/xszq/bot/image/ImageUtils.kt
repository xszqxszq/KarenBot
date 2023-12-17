package xyz.xszq.bot.image

import com.sksamuel.scrimage.AwtImage
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.JpegWriter
import korlibs.image.awt.AwtNativeImage
import korlibs.image.awt.toAwt
import korlibs.image.bitmap.Bitmap
import korlibs.image.bitmap.ensureNative
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO


fun BufferedImage.toARGB(): BufferedImage {
    val newImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val g = newImage.createGraphics()
    g.drawImage(this, 0, 0, null)
    g.dispose()
    return newImage
}
fun Bitmap.toImmutableImage(): ImmutableImage {
    return ImmutableImage.fromAwt(this.ensureNative().toAwt().toARGB())
}
fun Bitmap.toJPEG(): ByteArray {
    return toImmutableImage().bytes(JpegWriter().withCompression(75).withProgressive(true))
}


fun Mat.toBufferedImageOld(): BufferedImage {
    val mob = MatOfByte()
    Imgcodecs.imencode(".png", this, mob)
    return ImageIO.read(ByteArrayInputStream(mob.toArray()))
}
fun Mat.toBufferedImage(): BufferedImage {
    val imgSrc = listOf(this)
    val imgDst = listOf(Mat(height(), width(), CvType.CV_8UC4))
    Core.mixChannels(imgSrc, imgDst, MatOfInt(0, 2, 1, 1, 2, 0, 3, 3))
    val data = ByteArray(width() * height() * elemSize().toInt())
    imgDst[0].get(0, 0, data)

    val out = BufferedImage(width(), height(), BufferedImage.TYPE_4BYTE_ABGR)

    out.raster.setDataElements(0, 0, width(), height(), data)
    return out
}
fun ByteArray.toJPEG(): ByteArray = ImmutableImage.loader()
    .fromBytes(this)
    .bytes(JpegWriter().withCompression(75).withProgressive(true))


fun Float.radTo180Deg(): Float {
    if (this in -Math.PI .. Math.PI)
        return this
    var result = this
    return if (this > 0) {
        while (result > Math.PI)
            result -= (Math.PI * 2).toFloat()
        result
    } else {
        while (result < -Math.PI)
            result += (Math.PI * 2).toFloat()
        result
    }
}