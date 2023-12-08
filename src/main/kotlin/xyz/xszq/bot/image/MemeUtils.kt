package xyz.xszq.bot.image

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.AnimatedGif
import com.sksamuel.scrimage.nio.StreamingGifWriter
import com.soywiz.korim.awt.toBMP32
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.font.Font
import com.soywiz.korim.format.PNG
import com.soywiz.korim.format.encode
import com.soywiz.korim.vector.Context2d
import com.soywiz.korio.file.std.localCurrentDirVfs
import com.soywiz.korma.geom.Angle
import com.soywiz.korma.geom.vector.arc
import org.checkerframework.checker.units.qual.m
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.imgcodecs.Imgcodecs
import xyz.xszq.bot.maimai.MultiPlatformNativeSystemFontProvider
import xyz.xszq.config
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Duration
import javax.imageio.ImageIO
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.roundToInt


inline fun Bitmap.modify(
    antialiased: Boolean = true,
    doLock: Boolean = true,
    callback: Context2d.() -> Unit
): Bitmap {
    lock(doLock = doLock) {
        val ctx = getContext2d(antialiased)
        kotlin.runCatching {
            callback(ctx)
        }.onFailure {
            it.printStackTrace()
        }
        ctx.dispose()
    }
    return this
}
fun getAvgDuration(image: AnimatedGif): Double {
    if (image.frameCount == 1)
        return 0.0
    val totalDuration = List(image.frames.size) { index -> image.getDelay(index).toMillis().toDouble() }.sum()
    return totalDuration / image.frameCount
}
fun ImmutableImage.toBitmap(): Bitmap = toNewBufferedImage(BufferedImage.TYPE_INT_ARGB).toBMP32()
enum class FrameAlignPolicy {
    NoExtend, ExtendFirst, ExtendLast, ExtendLoop
}
suspend fun makeJpgOrGif(
    img: BuildImage,
    func: BuildImage.() -> BuildImage
): ByteArray {
    if (!img.isAnimated)
        return func(img).image.toJPEG()
    val duration = getAvgDuration(img.rawGifFile!!) / 1000.0
    val frames = splitGif(img.rawGifFile!!).map { frame -> func(BuildImage(frame)).image }
    return saveGif(frames, duration)
}
suspend fun makeGifOrCombinedGif(
    img: BuildImage,
    frameNum: Int,
    duration: Double,
    frameAlign: FrameAlignPolicy = FrameAlignPolicy.NoExtend,
    inputBased: Boolean = false,
    maker: BuildImage.(Int) -> BuildImage
): ByteArray {
    if (!img.isAnimated)
        return saveGif((0 until frameNum).map { i -> maker(img, i).image }, duration)
    val gifFile = img.rawGifFile!!
    val frameNumIn = gifFile.frameCount
    val durationIn = getAvgDuration(gifFile) / 1000.0
    val totalDurationIn = frameNumIn * durationIn
    val totalDuration = frameNum * duration

    val frameNumBase = if (inputBased) frameNumIn else frameNum
    val frameNumFit = if (inputBased) frameNum else frameNumIn
    val durationBase = if (inputBased) durationIn else duration
    val durationFit = if (inputBased) duration else durationIn
    val totalDurationBase = if (inputBased) totalDurationIn else totalDuration
    val totalDurationFit = if (inputBased) totalDuration else totalDurationIn

    var frameIdx = (0 until frameNumBase).toList()
    val diffDuration = totalDurationFit - totalDurationBase
    val diffNum = (diffDuration / durationBase).toInt()


    if (diffDuration >= durationBase)
        if (frameAlign == FrameAlignPolicy.ExtendFirst) {
            frameIdx = (0 until diffNum).map { 0 } + frameIdx
        } else if (frameAlign == FrameAlignPolicy.ExtendLast) {
            frameIdx = frameIdx + (0 until diffNum).map { frameNumBase - 1 }
        } else if (frameAlign == FrameAlignPolicy.ExtendLoop) {
            var frameNumTotal = frameNumBase
            while (frameNumTotal + frameNumBase <= config.gifMaxFrames) {
                frameNumTotal += frameNumBase
                frameIdx = frameIdx + (0 until frameNumBase).toList()
                val multiple = (frameNumTotal * durationBase / totalDurationFit).roundToInt()
                if ((totalDurationFit * multiple - frameNumTotal * durationBase).absoluteValue <= durationBase)
                    break
            }
        }
    val frames = mutableListOf<Bitmap>()
    var frameIdxFit = 0
    var timeStart = 0.0
    frameIdx.forEachIndexed { i, idx ->
        while (frameIdxFit < frameNumFit) {
            if (frameIdxFit * durationFit <= i * durationBase - timeStart &&
                i * durationBase - timeStart < (frameIdxFit + 1) * durationFit) {
                val idxIn = if (inputBased) idx else frameIdxFit
                val idxMaker = if (inputBased) frameIdxFit else idx
                frames.add(maker(BuildImage(gifFile.frames[idxIn].toBitmap()), idxMaker).image)
                break
            } else {
                frameIdxFit += 1
                if (frameIdxFit >= frameNumFit) {
                    frameIdxFit = 0
                    timeStart += totalDurationFit
                }
            }
        }
    }
    return saveGif(frames, duration)
}
fun splitGif(image: AnimatedGif): List<Bitmap> = image.frames.map { it.toBitmap() }

fun Bitmap.toMemeBuilder() = BuildImage(this)
fun List<String>.ifBlank(defaultValue: () -> String): String {
    if (this.isNotEmpty() && this.first().isNotBlank())
        return this.first()
    return defaultValue()
}
val globalFontRegistry = MultiPlatformNativeSystemFontProvider(localCurrentDirVfs["font"].absolutePath)
fun getProperFont(
    text: String,
    fontName: String? = null,
    fallbackFonts: List<String> = listOf(),
    defaultFontName: String = "Glow Sans SC Normal Book"
): Font {
    val fonts = fallbackFonts.toMutableList()
    fontName ?.let {
        fonts.add(0, it)
    }
    fonts.forEach { f ->
        globalFontRegistry.loadFontByName(f) ?.let { font ->
            if (text.all { font[it] != null })
                return font
        }
    }
    return globalFontRegistry.loadFontByName(defaultFontName)!!
}
suspend fun Bitmap.toImmutableImage(): ImmutableImage = ImmutableImage.loader().fromBytes(encode(PNG)).toImmutableImage()
suspend fun saveGif(frames: List<Bitmap>, duration: Double): ByteArray {
    val output = encodeGif(frames, (duration * 1000L).toLong())
    if (output.size <= config.gifMaxSize * 10.0.pow(6))
        return output
    if (frames.size > config.gifMaxFrames) {
        val ratio = 1.0 * frames.size / config.gifMaxFrames
        val index = (0 until config.gifMaxFrames).map { (it * ratio).toInt() }
        return saveGif(frames.filterIndexed { i, _ -> i in index }, duration * ratio)
    }
    return saveGif(frames.map { it.toBMP32().scaled((it.width * 0.9).toInt(), (it.height * 0.9).toInt()) }, duration)
}
suspend fun encodeGif(frames: List<Bitmap>, duration: Long, compressed: Boolean = false): ByteArray {
    val writer = StreamingGifWriter(Duration.ofMillis(duration), true, compressed)
    val os = ByteArrayOutputStream()
    writer.prepareStream(os, BufferedImage.TYPE_INT_ARGB).use { gifStream ->
        frames.forEach {
            gifStream.writeFrame(it.toImmutableImage())
        }
    }
    os.close()
    return os.toByteArray()
}

// https://answers.opencv.org/question/31505/how-load-and-display-images-with-java-using-opencv-solved/
fun Mat.toBufferedImage(): BufferedImage {
    val mob = MatOfByte()
    Imgcodecs.imencode(".png", this, mob)
    return ImageIO.read(ByteArrayInputStream(mob.toArray()))
}
fun getKorimCircle(size: Int, color: RGBA = Colors.WHITE, bg: RGBA = RGBA(0, 0, 0, 0)) =
    NativeImage(size, size).modify {
        fillStyle = bg
        fillRect(0, 0, width, height)
        beginPath()
        arc(size / 2, size / 2, size / 2, Angle.fromDegrees(0), Angle.fromDegrees(360))
        fillStyle = color
        fill()
    }