@file:Suppress("MemberVisibilityCanBePrivate")

package xyz.xszq.bot.image

import com.sksamuel.scrimage.filter.Filter
import com.sksamuel.scrimage.filter.GrayscaleFilter
import com.sksamuel.scrimage.filter.MotionBlurFilter
import com.sksamuel.scrimage.nio.AnimatedGif
import com.sksamuel.scrimage.nio.AnimatedGifReader
import com.sksamuel.scrimage.nio.ImageSource
import com.soywiz.korim.awt.toAwtNativeImage
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.font.Font
import com.soywiz.korim.font.TtfFont
import com.soywiz.korim.font.readTtfFont
import com.soywiz.korim.format.PNG
import com.soywiz.korim.format.encode
import com.soywiz.korim.format.readNativeImage
import com.soywiz.korim.text.HorizontalAlign
import com.soywiz.korim.text.VerticalAlign
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.std.localCurrentDirVfs
import com.soywiz.korio.net.MimeType
import com.soywiz.korio.net.mimeType
import com.soywiz.korma.geom.Size
import com.soywiz.korma.geom.degrees
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.core.CvType.CV_32FC3
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import xyz.xszq.nereides.imageType
import xyz.xszq.nereides.radTo180Deg
import java.io.File
import kotlin.math.*

class BuildImage(var image: Bitmap) {
    var rawGifFile: AnimatedGif? = null
    enum class DirectionType {
        Center,
        North,
        South,
        West,
        East,
        Northwest,
        Northeast,
        Southwest,
        Southeast,
    }
    val width
        get() = image.width
    val height
        get() = image.height
    val size
        get() = image.size
    var mode = "RGBA"
    val isAnimated
        get() = rawGifFile != null
    fun copy() = BuildImage(image.clone())
    suspend fun convert(mode: String) = when(mode) {
        "L" -> {
            BuildImage(image.toImmutableImage().filter(GrayscaleFilter()).toBitmap())
        }
        else -> copy().apply {
            this.mode = mode
        }
    }
    fun resize(
        size: Size,
        resample: Boolean = true,
        keepRatio: Boolean = false,
        inside: Boolean = false,
        direction: DirectionType = DirectionType.Center,
        bgColor: RGBA? = null
    ): BuildImage {
        var width = size.width
        var height = size.height
        if (keepRatio) {
            val ratio = listOf(width / this.width, height / this.height).run {
                if (inside)
                    this.min()
                else
                    this.max()
            }
            width = this.width * ratio
            height = this.height * ratio
        }
        var image = BuildImage(this.image.toBMP32().scaled(width.toInt(), height.toInt(), resample))
        if (keepRatio)
            image = image.resizeCanvas(size, direction, bgColor)
        return image
    }
    fun resizeCanvas(
        size: Size,
        direction: DirectionType = DirectionType.Center,
        bgColor: RGBA? = null
    ): BuildImage {
        val w = size.width
        val h = size.height
        var x = (w - this.width) / 2
        var y = (h - this.height) / 2
        if (direction in listOf(DirectionType.North, DirectionType.Northwest, DirectionType.Northeast))
            y = 0.0
        else if (direction in listOf(DirectionType.South, DirectionType.Southwest, DirectionType.Southeast))
            y = h - this.height
        if (direction in listOf(DirectionType.West, DirectionType.Northwest, DirectionType.Southwest))
            x = 0.0
        else if (direction in listOf(DirectionType.East, DirectionType.Northeast, DirectionType.Southeast))
            y = w - this.width
        val image = new(this.mode, size, bgColor)
        image.paste(this.image, Pair(x.toInt(), y.toInt()))
        return image
    }
    fun resizeWidth(
        width: Int,
        resample: Boolean = true,
        keepRatio: Boolean = false,
        inside: Boolean = false,
        direction: DirectionType = DirectionType.Center,
        bgColor: RGBA? = null
    ) = resize(Size(width, this.height * width / this.width), resample, keepRatio, inside, direction, bgColor)
    fun heightIfResized(width: Int) = this.height * width / this.width
    fun widthIfResized(height: Int) = this.width * height / this.height
    fun resizeHeight(
        height: Int,
        resample: Boolean = true,
        keepRatio: Boolean = false,
        inside: Boolean = false,
        direction: DirectionType = DirectionType.Center,
        bgColor: RGBA? = null
    ) = resize(Size(this.width * height / this.height, height), resample, keepRatio, inside, direction, bgColor)
    fun rotate(angle: Double, expand: Boolean = false): BuildImage {
        val beta = (-angle).degrees.radians.radTo180Deg()
        val alpha = beta.absoluteValue
        val newWidth = image.width * cos(alpha).absoluteValue + image.height * sin(alpha)
        val newHeight = image.width * sin(alpha) + image.height * cos(alpha).absoluteValue
        val xOffset = when {
            beta >= Math.PI / 2.0 -> newWidth
            beta <= -Math.PI / 2.0 -> width * cos(alpha).absoluteValue
            beta > 0.0 -> image.height * sin(alpha)
            else -> 0.0
        }
        val yOffset = when {
            beta >= Math.PI / 2.0 -> height * cos(alpha).absoluteValue
            beta <= -Math.PI / 2.0 -> newHeight
            beta < 0.0 -> image.width * sin(alpha)
            else -> 0.0
        }
        return BuildImage(when {
            !expand -> NativeImage(width, height)
            else -> NativeImage(newWidth.toInt(), newHeight.toInt())
        }.modify {
            save()
            when {
                !expand -> translate(xOffset - (newWidth - width) / 2.0, yOffset - (newHeight - height) / 2.0)
                else -> translate(xOffset, yOffset)
            }
            rotate(beta)
            drawImage(image, 0, 0)
            restore()
        })
    }
    fun crop(bounds: List<Int>): BuildImage {
        return BuildImage(NativeImage(bounds[2] - bounds[0], bounds[3] - bounds[1]).modify {
            fillStyle = Colors.WHITE
            fillRect(0, 0, this.width, this.height)
            drawImage(image, -bounds[0], -bounds[1])
        })
    }
    fun square(): BuildImage {
        val length = min(width, height)
        return resizeCanvas(Size(length, length))
    }
    suspend fun circle(): BuildImage {
        val image = square().convert("RGBA").image
        val circle = getKorimCircle(max(image.width, image.height))

        image.forEach { _, x, y ->
            if (circle.getRgba(x, y).a == 0)
                image.setRgba(x, y, RGBA(0, 0, 0, 0))
        }
        return BuildImage(image)
    }
    suspend fun circleCorner(r: Double): BuildImage {
        val image = convert("RGBA").image
        val circle = getKorimRoundedRectangle(r, width, height)

        image.forEach { _, x, y ->
            if (circle.getRgba(x, y).a == 0)
                image.setRgba(x, y, RGBA(0, 0, 0, 0))
        }
        return BuildImage(image)
    }
    fun paste(
        img: Bitmap,
        pos: Pair<Int, Int> = Pair(0, 0),
        alpha: Boolean = false, // Useless, since all Bitmap is RGBA
        below: Boolean = false,
        bgColor: RGBA? = null
    ): BuildImage {
        val newImage = if (below) NativeImage(width, height) else this.image.clone()
        newImage.modify {
            bgColor ?.let {
                fillStyle = bgColor
                fillRect(0, 0, this.width, this.height)
            }
            drawImage(img, pos.first, pos.second)
        }
        if (below)
            newImage.modify {
                drawImage(this@BuildImage.image, 0, 0)
            }
        this.image = newImage
        return this
    }
    fun paste(
        img: BuildImage,
        pos: Pair<Int, Int> = Pair(0, 0),
        alpha: Boolean = false,
        below: Boolean = false
    ) = paste(img.image, pos, alpha, below)
    fun drawText(
        xy: List<Int>,
        text: String,
        fontSize: Int = 16,
        maxFontSize: Int = 16,
        minFontSize: Int = 16,
        allowWrap: Boolean = false,
        fill: RGBA = Colors.BLACK,
        spacing: Int = 4,
        hAlign: HorizontalAlign = HorizontalAlign.CENTER,
        vAlign: VerticalAlign = VerticalAlign.MIDDLE,
        linesAlign: HorizontalAlign = HorizontalAlign.LEFT,
        strokeRatio: Double = 0.0,
        strokeFill: RGBA? = null,
        fontFallback: Boolean = true,
        fontName: String? = null,
        fallbackFonts: List<String> = defaultFallbackFonts
    ): BuildImage {
        if (xy.size == 2) {
            Text2Image.fromText(
                text, fontSize, fill, spacing, linesAlign, (fontSize * strokeRatio).toInt(), strokeFill,
                fontFallback, fontName, fallbackFonts
            ).drawOnImage(image, Pair(xy.first().toDouble(), xy.last().toDouble()))
            return this
        }
        val left = xy[0]
        val top = xy[1]
        val width = xy[2] - xy[0]
        val height = xy[3] - xy[1]
        var nowFontSize = maxFontSize
        while (true) {
            val text2Image = Text2Image.fromText(
                text, nowFontSize, fill, spacing, linesAlign, (nowFontSize * strokeRatio).toInt(), strokeFill,
                fontFallback, fontName, fallbackFonts
            )
            var textW = text2Image.width
            var textH = text2Image.height
            if (textW > width && allowWrap) {
                text2Image.wrap(width.toDouble())
                textW = text2Image.width
                textH = text2Image.height
            }
            if (textW > width || textH > height) {
                nowFontSize -= 1
                if (nowFontSize < minFontSize)
                    throw Exception("在指定的区域和字体大小范围内画不下这段文字")
            } else {
                var x = left.toDouble()
                if (hAlign == HorizontalAlign.CENTER)
                    x += (width - textW) / 2
                else if (hAlign == HorizontalAlign.RIGHT)
                    x += width - textW
                var y = top.toDouble()
                if (vAlign == VerticalAlign.MIDDLE)
                    y += (height - textH) / 2
                else if (vAlign == VerticalAlign.BOTTOM)
                    y += height - textH

                text2Image.drawOnImage(image, Pair(x, y))
                return this
            }
        }
    }
    suspend fun perspective(points: MatOfPoint2f): BuildImage {
        val pointsW = points.toList().map { it.x }
        val pointsH = points.toList().map { it.y }
        val newW = pointsW.max() - pointsW.min()
        val newH = pointsH.max() - pointsH.min()
        val warpMat = Imgproc.getPerspectiveTransform(MatOfPoint2f(
            Point(0.0, 0.0),
            Point(width.toDouble(), 0.0),
            Point(width.toDouble(), height.toDouble()),
            Point(0.0, height.toDouble())
        ), points)
        val destImage = Mat()
        Imgproc.warpPerspective(toMat(), destImage, warpMat, org.opencv.core.Size(newW, newH))
        return BuildImage(destImage.toBufferedImage().toAwtNativeImage())
    }
    suspend fun perspective(points: List<Pair<Int, Int>>): BuildImage = perspective(
        MatOfPoint2f(
            Point(points[0].first.toDouble(), points[0].second.toDouble()),
            Point(points[1].first.toDouble(), points[1].second.toDouble()),
            Point(points[2].first.toDouble(), points[2].second.toDouble()),
            Point(points[3].first.toDouble(), points[3].second.toDouble())
        )
    )
    suspend fun motionBlur(angle: Double = 0.0, degree: Int): BuildImage {
        if (degree == 0)
            return copy()
        return BuildImage(
            image.toImmutableImage()
                .filter(MotionBlurFilter(angle, degree.toDouble(), 0.0, 0.0))
                .toBitmap())
    }
    suspend fun toMat(): Mat = withContext(Dispatchers.IO) {
        val bytes = MatOfByte()
        bytes.fromList(image.encode(PNG).toList())
        Imgcodecs.imdecode(bytes, Imgcodecs.IMREAD_UNCHANGED)
    }
    fun drawLine(xy: List<Double>, fill: RGBA = Colors.WHITE, width: Double = 1.0): BuildImage { //TODO: FIX THIS
        image.modify {
            fillStyle = fill
//            lineWidth = width
            fillRect(xy[0], xy[1] - width, xy[2] - xy[0], xy[3] - xy[1] + width)
//            beginPath()
//            moveTo(xy[0], xy[1])
//            lineTo(xy[2], xy[3])
//            close()
//            fill()
        }
        return this
    }
    suspend fun colorMask(color: RGBA): BuildImage { // TODO: Fix this
        val img = toMat()
        val imgGray = Mat()
        Imgproc.cvtColor(img, imgGray, Imgproc.COLOR_RGB2GRAY)
        val imgHsl = Mat()
        Imgproc.cvtColor(img, imgHsl, Imgproc.COLOR_RGB2HLS)
        val imgNew = Mat.zeros(height, width, CV_32FC3)
        val r = color.b
        val g = color.g
        val b = color.r
        val rgbSum = r + g + b
        for (i in 0 until height) {
            for (j in 0 until width) {
                val value = imgGray[i, j][0]
                imgNew.put(i, j, if (rgbSum != 0) floatArrayOf(
                    (value * r / rgbSum).toFloat(),
                    (value * g / rgbSum).toFloat(),
                    (value * b / rgbSum).toFloat()
                ) else floatArrayOf(0.0f, 0.0f, 0.0f))
            }
        }
        val imgNewHsl = Mat()
        Imgproc.cvtColor(imgNew, imgNewHsl, Imgproc.COLOR_RGB2HLS)
        val result = Mat(height, width, CV_32FC3)
        for (i in 0 until height) {
            for (j in 0 until width) {
                result.put(i, j, floatArrayOf(
                    imgNewHsl[i, j][0].toFloat(),
                    imgHsl[i, j][1].toFloat(),
                    imgNewHsl[i, j][2].toFloat()
                ))
            }
        }
        Imgproc.cvtColor(result, result, Imgproc.COLOR_HLS2RGB)
        return BuildImage(result.toBufferedImage().toAwtNativeImage())
    }
    suspend fun filter(filter: Filter): BuildImage = BuildImage(image.toImmutableImage().filter(filter).toBitmap())

    suspend fun save(format: String): ByteArray = when (format) {
        "png" -> image.encode(PNG)
        in listOf("jpg", "jpeg") -> image.toJPEG()
        else -> image.encode(PNG)
    }
    suspend fun saveJpg(bgColor: RGBA = Colors.WHITE): ByteArray {
        val img = if (mode == "RGBA") {
            new("RGBA", size, bgColor).paste(image)
        } else this
        return img.save("jpeg")
    }
    suspend fun savePng(bgColor: RGBA = Colors.WHITE): ByteArray {
        val img = if (mode == "RGBA") {
            new("RGBA", size, bgColor).paste(image)
        } else this
        return img.save("png")
    }
    companion object {
        val fonts = mutableMapOf<String, TtfFont>()
        suspend fun init() {
            val fontDir = localCurrentDirVfs["font"]
            fontDir.list().collect {
                val font = it.readTtfFont()
                fonts[font.ttfName] = font
            }
        }
        fun new(mode: String, size: Size, color: RGBA? = null) =
            BuildImage(NativeImage(size.width.toInt(), size.height.toInt()).modify {
                if (mode != "RGBA" && color == null) {
                    fillStyle = Colors.BLACK
                    fillRect(0, 0, size.width, size.height)
                }
                color ?.let {
                    fillStyle = color
                    fillRect(0, 0, size.width, size.height)
                }
            }).apply {
                this.mode = mode
            }
        suspend fun open(file: VfsFile): BuildImage {
            if (file.mimeType() == MimeType.IMAGE_GIF) {
                val gifFile = AnimatedGifReader.read(ImageSource.of(File(file.absolutePath)))
                val builder = BuildImage(gifFile.frames.first().toBitmap())
                builder.rawGifFile = gifFile
                return builder
            }
            return BuildImage(file.readNativeImage())
        }
        val defaultFallbackFonts = listOf(
            "Glow Sans SC Normal Book",
            "Glow Sans SC Normal Regular",
            "Glow Sans SC Normal Bold",
            "Glow Sans SC Normal Heavy",
            "Source Han Sans CN Bold",
            "Source Han Serif SC Bold"
        )
        fun getProperFont(
            text: String,
            fontName: String? = null,
            fallbackFonts: List<String> = defaultFallbackFonts,
            defaultFontName: String = "Glow Sans SC Normal Book"
        ): Font {
            val fonts = fallbackFonts.toMutableList()
            fontName ?.let {
                fonts.add(0, it)
            }
            fonts.forEach { f ->
                BuildImage.fonts[f] ?.let { font ->
                    if (!text.any { font[it] == null })
                        return font
                }
            }
            return BuildImage.fonts[defaultFontName]!!
        }
    }
}