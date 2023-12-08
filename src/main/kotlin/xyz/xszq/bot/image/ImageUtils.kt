package xyz.xszq.bot.image

import com.sksamuel.scrimage.AwtImage
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.JpegWriter
import com.soywiz.korim.awt.AwtNativeImage
import com.soywiz.korim.awt.toAwt
import com.soywiz.korim.awt.toAwtNativeImage
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.format.PNG
import com.soywiz.korim.format.encode


fun AwtNativeImage.toImmutableImage(): ImmutableImage = AwtImage(awtImage).toImmutableImage()
suspend fun Bitmap.toJPEG(): ByteArray = ImmutableImage.loader()
    .fromBytes(this.encode(PNG))
    .bytes(JpegWriter().withCompression(75).withProgressive(true))

fun ByteArray.toJPEG(): ByteArray = ImmutableImage.loader()
    .fromBytes(this)
    .bytes(JpegWriter().withCompression(75).withProgressive(true))