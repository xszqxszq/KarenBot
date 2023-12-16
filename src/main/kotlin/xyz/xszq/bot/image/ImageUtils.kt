package xyz.xszq.bot.image

import com.sksamuel.scrimage.AwtImage
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.JpegWriter
import korlibs.image.awt.AwtNativeImage
import korlibs.image.bitmap.Bitmap
import korlibs.image.format.PNG
import korlibs.image.format.encode


fun AwtNativeImage.toImmutableImage(): ImmutableImage = AwtImage(awtImage).toImmutableImage()
suspend fun Bitmap.toJPEG(): ByteArray = ImmutableImage.loader()
    .fromBytes(this.encode(PNG))
    .bytes(JpegWriter().withCompression(75).withProgressive(true))

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