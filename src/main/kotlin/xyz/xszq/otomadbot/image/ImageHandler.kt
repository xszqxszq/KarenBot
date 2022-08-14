package xyz.xszq.otomadbot.image

import ai.djl.modality.cv.output.DetectedObjects.DetectedObject
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.sksamuel.scrimage.ImmutableImage
import com.soywiz.korim.awt.toAwtNativeImage
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.format.PNG
import com.soywiz.korim.format.readImageInfo
import com.soywiz.korim.format.readNativeImage
import com.soywiz.korim.format.writeTo
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.std.toVfs
import com.twelvemonkeys.imageio.plugins.webp.WebPImageReaderSpi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.contact.isOperator
import net.mamoe.mirai.event.AbstractEvent
import net.mamoe.mirai.event.broadcast
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.message.data.ImageType
import net.mamoe.mirai.message.data.MarketFace
import net.mamoe.mirai.message.data.MessageSource.Key.recall
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import net.mamoe.mirai.utils.MiraiInternalApi
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import xyz.xszq.OtomadBotCore
import xyz.xszq.events
import xyz.xszq.otomadbot.Command
import xyz.xszq.otomadbot.CommandModule
import xyz.xszq.otomadbot.NetworkUtils
import xyz.xszq.otomadbot.kotlin.isUrl
import xyz.xszq.otomadbot.kotlin.newTempFile
import xyz.xszq.otomadbot.mirai.queryUrl
import xyz.xszq.otomadbot.mirai.quoteReply
import java.awt.image.BufferedImage
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.math.pow
import kotlin.random.Random


class ReplyPicList {
    val list = HashMap<String, ArrayList<Path>>()
    val included = mutableListOf<String>()
    fun load(dir: String, target: String = dir) {
        if (!list.containsKey(target)) {
            list[target] = arrayListOf()
            included.add(target)
        }
        if (!OtomadBotCore.configFolder.resolve("image").exists())
            OtomadBotCore.configFolder.resolve("image").mkdir()
        if (!OtomadBotCore.configFolder.resolve("image/$dir").exists())
            OtomadBotCore.configFolder.resolve("image/$dir").mkdir()
        Files.walk(OtomadBotCore.configFolder.resolve("image/$dir").toPath(), 2)
            .filter { i -> Files.isRegularFile(i) }
            .forEach { path -> list[target]!!.add(path) }
    }
    fun getRandom(dir: String): File = list[dir]!![Random.nextInt(list[dir]!!.size)].toFile()
}

class ImageReceivedEvent(
    val img: List<File>,
    val event: MessageEvent
): AbstractEvent()

object ImageHandler: CommandModule("图片检测", "image.common") {
    val yoloCommon = YOLOv5("common")
    val yoloFace = YOLOv5("anime_face")

    val skin = doubleArrayOf(250.6755287, 233.2529708, 219.23726083).toLab()
    val replyPic = ReplyPicList()
    @OptIn(MiraiInternalApi::class)
    override suspend fun subscribe() {
        yoloCommon.init()
        yoloFace.init()
        // 遇到图片下载下来再广播，也能给其他需要图片的模块用。GIF截第一帧（待改进）
        events.subscribeAlways<MessageEvent> {
            val files = message.filterIsInstance<Image>().filter { it.imageType != ImageType.GIF }.mapNotNull {
                NetworkUtils.downloadTempFile(it.queryUrl())
            }.toMutableList()
            files.addAll(message.filterIsInstance<MarketFace>().mapNotNull {
                NetworkUtils.downloadTempFile(it.queryUrl())
            })
            val images = files.toMutableList()
            message.filterIsInstance<Image>().filter { it.imageType == ImageType.GIF }.mapNotNull {
                NetworkUtils.downloadTempFile(it.queryUrl())
            }.forEach {
                files.add(it)
                val firstFrame = newTempFile()
                it.toVfs().readNativeImage().writeTo(firstFrame.toVfs(), PNG)
                files.add(firstFrame)
                images.add(firstFrame)
            }
            ImageReceivedEvent(images, this).broadcast()
            files.forEach { it.deleteOnExit() }
        }
        // 下好了再分开异步处理
        events.subscribeAlways<ImageReceivedEvent> { event ->
            withContext(Dispatchers.IO) {
                blondeDetect.checkAndRun(event)
            }
        }
        events.subscribeAlways<ImageReceivedEvent> { event ->
            withContext(Dispatchers.IO) {
                qrScan.checkAndRun(event)
            }
        }
        events.subscribeAlways<ImageReceivedEvent> {
            longDetect.checkAndRun(this)
        }
    }
    val longDetect = Command<ImageReceivedEvent>("龙图检测", "long", false) {
        img.forEach { image ->
            val native = image.toVfs().readAsImage()
            if (native.width >= 16 && native.height >= 16) {
                val common = yoloCommon.detect(image).items<DetectedObject>()
                if (common.any { c -> c.className == "longyutao_face" && c.probability >= 0.8 }) {
                    if (event is GroupMessageEvent && event.group.botAsMember.isOperator()) {
                        event.message.recall()
                    } else {
                        event.quoteReply("我超，龙")
                    }
                }
            }
        }
    }
    val blondeDetect = Command<ImageReceivedEvent>("黄发检测", "blonde") {
        if (img.any { image ->
                detectBlondeHair(image, Imgcodecs.imread(image.absolutePath), this)
            } || img.any { ImageMatcher.matchImage("reply", it) }) {
            replyPic.getRandom("reply").toExternalResource().use {
                event.subject.sendMessage(it.uploadAsImage(event.subject))
            }
        }
    }
    val qrScan = Command<ImageReceivedEvent>("二维码扫描", "qrscan") {
        kotlin.runCatching {
            val list = img.map { it.decodeQR() }.filter { it.isUrl() }
            if (list.isNotEmpty())
                event.quoteReply(list.joinToString("\n"))
        }
    }
    val targetHairColors = listOf(
        doubleArrayOf(90.73948779100502,-4.477169141775095, 42.932090784625785),
        doubleArrayOf(96.01817010098311, -7.205140542378363, 28.57951059226316),
        doubleArrayOf(81.3990919790363, 10.1113281133971, 23.247659861636638),
        doubleArrayOf(95.85821436038806, -19.323659509977777, 67.48143654894507)
    )
    suspend fun detectBlondeHair(file: File, image: Mat, event: ImageReceivedEvent) = event.run {
        yoloFace.detect(file).items<DetectedObject>().any {
            val bounds = it.boundingBox.bounds
            val realY = if (bounds.y.toInt() < 20) 1.0 else bounds.y - 20
            val fringe = withContext(Dispatchers.IO) {
                Mat(image, Rect(bounds.x.toInt(), realY.toInt(), bounds.width.toInt(),
                    (bounds.height + bounds.y.toInt() - realY).toInt() / 4
                )).clone()
            }

            val hair = getHairColor(fringe)
            hair.any { h -> targetHairColors.any { e -> h.dis(e) < 100 } }
        }
    }

    suspend fun getHairColor(image: Mat) = withContext(Dispatchers.IO) {
        val samples = image.reshape(1, image.cols() * image.rows())
        val samples32f = Mat()
        samples.convertTo(samples32f, CvType.CV_32F, 1.0 / 255)
        val labels = Mat()
        val centers = Mat()
        val criteria = TermCriteria(TermCriteria.COUNT, 100, 1.0)
        Core.kmeans(samples32f, 6, labels, criteria, 1, Core.KMEANS_PP_CENTERS, centers)

        centers.convertTo(centers, CvType.CV_8UC1, 255.0)
        centers.reshape(3)

        val counts = buildMap {
            for (i in 0 until centers.rows())
                put(i, 0)
        }.toMutableMap()

        var rows = 0
        for (y in 0 until image.rows()) {
            for (x in 0 until image.cols()) {
                val label = labels[rows, 0][0].toInt()
                counts[label] = counts[label]!! + 1
                rows++
            }
        }
        val colors = counts.toList().sortedByDescending { it.second }.map {
            doubleArrayOf(centers[it.first, 2][0], centers[it.first, 1][0], centers[it.first, 0][0]).toLab()
        }.filter { it.dis(skin) >= 100 }
        return@withContext colors.take(2)
    }
}

fun File.decodeQR(): String {
    val binarizer = HybridBinarizer(BufferedImageLuminanceSource(ImageIO.read(FileInputStream(this))))
    val decodeHints = HashMap<DecodeHintType, Any?>()
    decodeHints[DecodeHintType.CHARACTER_SET] = "UTF-8"
    val result = MultiFormatReader().decode(BinaryBitmap(binarizer), decodeHints)
    return result.text
}

// Reference: https://stackoverflow.com/a/45263428/12944612
fun DoubleArray.toLab(): DoubleArray {
    // --------- RGB to XYZ ---------//
    var r = this[0] / 255.0
    var g = this[1] / 255.0
    var b = this[2] / 255.0
    r = if (r > 0.04045) ((r + 0.055) / 1.055).pow(2.4) else r / 12.92
    g = if (g > 0.04045) ((g + 0.055) / 1.055).pow(2.4) else g / 12.92
    b = if (b > 0.04045) ((b + 0.055) / 1.055).pow(2.4) else b / 12.92
    r *= 100.0
    g *= 100.0
    b *= 100.0
    val x = 0.4124 * r + 0.3576 * g + 0.1805 * b
    val y = 0.2126 * r + 0.7152 * g + 0.0722 * b
    val z = 0.0193 * r + 0.1192 * g + 0.9505 * b
    // --------- XYZ to Lab --------- //
    var xr = x / 95.047
    var yr = y / 100.0
    var zr = z / 108.883
    xr = if (xr > 0.008856) xr.pow(1 / 3.0) else ((7.787 * xr) + 16 / 116.0)
    yr = if (yr > 0.008856) yr.pow(1 / 3.0) else ((7.787 * yr) + 16 / 116.0)
    zr = if (zr > 0.008856) zr.pow(1 / 3.0) else ((7.787 * zr) + 16 / 116.0)
    val lab = DoubleArray(3)
    lab[0] = 116 * yr - 16
    lab[1] = 500 * (xr - yr)
    lab[2] = 200 * (yr - zr)
    return lab
}
fun DoubleArray.dis(target: DoubleArray): Double {
    return (this[0] - target[0]).pow(2) + (this[1] - target[1]).pow(2) + (this[2]-target[2]).pow(2)
}

object ImageDecodeUtils {
    val webp: suspend VfsFile.() -> NativeImage = {
        withContext(Dispatchers.IO) {
            val reader = WebPImageReaderSpi().createReaderInstance()
            reader.input = ImageIO.createImageInputStream(File(absolutePath).inputStream())
            reader.read(0).toAwtNativeImage()
        }
    }
    val anyByAwt: suspend VfsFile.() -> NativeImage = {
        readNativeImage()
    }
    val anyByScrimage: suspend VfsFile.() -> NativeImage = {
        ImmutableImage.loader().fromFile(File(absolutePath)).awt().toAwtNativeImage()
    }
    val formats = listOf(anyByAwt, webp, anyByScrimage)
}

suspend fun VfsFile.readAsImage(): NativeImage {
    ImageDecodeUtils.formats.forEach {
        kotlin.runCatching {
            it.invoke(this)
        }.onSuccess {
            return it
        }
    }
    throw Exception()
}


fun BufferedImage.toInputStream(): InputStream? {
    val stream = ByteArrayOutputStream()
    return try {
        ImageIO.write(this, "png", stream)
        ByteArrayInputStream(stream.toByteArray())
    } catch (e: Exception) {
        null
    }
}