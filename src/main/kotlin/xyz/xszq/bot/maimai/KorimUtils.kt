package xyz.xszq.bot.maimai

import com.soywiz.kds.atomic.kdsFreeze
import com.soywiz.klock.measureTime
import com.soywiz.klock.measureTimeWithResult
import com.soywiz.klock.milliseconds
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.sliceWithSize
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.font.*
import com.soywiz.korim.paint.Paint
import com.soywiz.korim.text.HorizontalAlign
import com.soywiz.korim.text.TextAlignment
import com.soywiz.korim.vector.Context2d
import com.soywiz.korio.async.runBlockingNoJs
import com.soywiz.korio.concurrent.atomic.KorAtomicRef
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.baseName
import com.soywiz.korio.file.std.localVfs
import com.soywiz.korio.lang.Environment
import com.soywiz.korio.lang.expand
import xyz.xszq.nereides.hexToRGBA


private val linuxFolders get() = listOf("/usr/share/fonts", "/usr/local/share/fonts", "~/.fonts")
private val windowsFolders get() = listOf("%WINDIR%\\Fonts", "%LOCALAPPDATA%\\Microsoft\\Windows\\Fonts")
private val macosFolders get() = listOf("/System/Library/Fonts/", "/Library/Fonts/", "~/Library/Fonts/", "/Network/Library/Fonts/")
private val iosFolders get() = listOf("/System/Library/Fonts/Cache", "/System/Library/Fonts")
private val androidFolders get() = listOf("/system/Fonts", "/system/font", "/data/fonts")

// TODO: Remove this after korim fixed the bug
open class MultiPlatformNativeSystemFontProvider(
    private val customPath: String,
    private val folders: List<String> = linuxFolders + windowsFolders + macosFolders + androidFolders + iosFolders
            + listOf(customPath),
    private val fontCacheFile: String = "~/.korimFontCache"
) : TtfNativeSystemFontProvider() {
    private fun listFontNamesMap(): Map<String, VfsFile> = runBlockingNoJs {
        val out = LinkedHashMap<String, VfsFile>()
        val time = measureTime {
            val fontCacheVfsFile = localVfs(Environment.expand(fontCacheFile))
            val fileNamesToName = LinkedHashMap<String, String>()
            val oldFontCacheVfsFileText = try {
                fontCacheVfsFile.readString()
            } catch (e: Throwable) {
                ""
            }
            for (line in oldFontCacheVfsFileText.split("\n")) {
                val (file, name) = line.split("=", limit = 2) + listOf("", "")
                fileNamesToName[file] = name
            }
            for (folder in folders) {
                try {
                    val file = localVfs(Environment.expand(folder))
                    for (f in file.listRecursiveSimple()) {
                        try {
                            val name = fileNamesToName.getOrPut(f.baseName) {
                                val (ttf, _) = measureTimeWithResult { TtfFont.readNames(f) }
                                //if (totalTime >= 1.milliseconds) println("Compute name size[${f.size()}] '${ttf.ttfCompleteName}' $totalTime")
                                ttf.ttfCompleteName
                            }
                            //println("name=$name, f=$f")
                            if (name != "") {
                                out[name] = f
                            }
                        } catch (e: Throwable) {
                            fileNamesToName.getOrPut(f.baseName) { "" }
                        }
                    }
                } catch (_: Throwable) {
                }
            }
            val newFontCacheVfsFileText = fileNamesToName.map { "${it.key}=${it.value}" }.joinToString("\n")
            if (newFontCacheVfsFileText != oldFontCacheVfsFileText) {
                try {
                    fontCacheVfsFile.writeString(newFontCacheVfsFileText)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
        if (time >= 100.milliseconds) {
            println("Load System font names in $time")
        }
        //println("fileNamesToName: $fileNamesToName")
        out
    }

    private fun listFontNamesMapLC(): Map<String, VfsFile> = listFontNamesMap().mapKeys { it.key.normalizeName() }
    override fun defaultFont(): TtfFont = DefaultTtfFont

    override fun listFontNamesWithFiles(): Map<String, VfsFile> = listFontNamesMap()

    private val _namesMapLC = KorAtomicRef<Map<String, VfsFile>?>(null)
    private val namesMapLC: Map<String, VfsFile> get() {
        if (_namesMapLC.value == null) {
            _namesMapLC.value = kdsFreeze(listFontNamesMapLC())
        }
        return _namesMapLC.value!!
    }

    override fun loadFontByName(name: String, freeze: Boolean): TtfFont? =
        runBlockingNoJs { namesMapLC[name.normalizeName()]?.let { TtfFont(it.readAll(), freeze = freeze) } }
}
fun Font.ellipsize(rawText: String, fontSize: Double, maxWidth: Double, xScale: Double = 1.0): String {
    var metrics = measureTextGlyphs(fontSize, rawText)
    if (metrics.metrics.width * xScale < maxWidth)
        return rawText
    var text = rawText
    while (text.isNotEmpty()) {
        metrics = measureTextGlyphs(fontSize, "$text...")
        if (metrics.metrics.width * xScale < maxWidth)
            return "$text..."
        text = text.substring(0 until text.length - 1)
    }
    return "$text..."
}
fun Context2d.drawText(
    rawText: String,
    x: Double,
    y: Double,
    color: RGBA = Colors.BLACK,
    font: TtfFont,
    fontSize: Double,
    align: TextAlignment = TextAlignment.LEFT,
    xScale: Double = 1.0,
    stroke: Double = 0.0,
    strokeColor: RGBA = Colors.BLACK,
    ellipsizeWidth: Double? = null
) {
    this.font = font
    this.fontSize = fontSize
    this.alignment = align
    if (stroke != 0.0) {
        this.lineWidth = stroke
        this.strokeStyle = strokeColor
    } else {
        this.lineWidth = 0.0
    }
    // TODO: Submit issue to korim to fix alignment, this is only a TEMPORARY solution
    val text = ellipsizeWidth ?.let {
        font.ellipsize(rawText, fontSize, ellipsizeWidth, xScale)
    } ?: rawText
    if (xScale != 1.0) {
        drawTextWithXScale(text, x, y, xScale, createColor(color), createColor(strokeColor))
    } else {
        val offsetX = when (this.alignment.horizontal) {
            HorizontalAlign.RIGHT -> -1 * (font.measureTextGlyphs(fontSize, text).metrics.width)
            HorizontalAlign.CENTER -> -1 * (font.measureTextGlyphs(fontSize, text).metrics.width / 2)
            else -> 0.0
        }
        if (stroke != 0.0)
            strokeText(text, x + offsetX, y)
        this.fillStyle = createColor(color)
        fillText(text, x + offsetX, y)
    }
    this.lineWidth = 0.0
}
fun Context2d.drawTextWithXScale(
    text: String,
    startX: Double,
    y: Double,
    xScale: Double = 1.0,
    fillStyle: Paint,
    strokeStyle: Paint
) {
    val offsetX = when (this.alignment.horizontal) {
        HorizontalAlign.RIGHT -> -1 * (font!!.measureTextGlyphs(fontSize, text).metrics.width * xScale)
        HorizontalAlign.CENTER -> -1 * (font!!.measureTextGlyphs(fontSize, text).metrics.width * xScale / 2)
        else -> 0.0
    }
    var x = startX + offsetX
    text.forEach { c ->
        val metrics = font!!.measureTextGlyphs(fontSize, c.toString())
        if (lineWidth != 0.0) {
            this.strokeStyle = strokeStyle
            strokeText(c.toString(), x, y)
        }
        this.fillStyle = fillStyle
        fillText(c.toString(), x, y)
        x += metrics.glyphs.first().metrics.xadvance * xScale
    }
}

fun Context2d.drawText(
    text: String,
    attr: ItemProperties,
    align: TextAlignment = TextAlignment.LEFT,
    ellipsizeWidth: Double? = null
) = drawText(
    text,
    attr.x.toDouble(),
    attr.y.toDouble(),
    attr.color.hexToRGBA(),
    MaimaiImage.fonts[attr.fontName]!!,
    attr.size.toDouble(),
    align,
    attr.xScale,
    attr.stroke,
    attr.strokeColor.hexToRGBA(),
    ellipsizeWidth
)
fun Context2d.drawTextRelative(
    text: String,
    x: Int,
    y: Int,
    attr: ItemProperties,
    align: TextAlignment = TextAlignment.LEFT,
    ellipsizeWidth: Double? = null
)  = drawText(
    text,
    x + attr.x.toDouble(),
    y + attr.y.toDouble(),
    attr.color.hexToRGBA(),
    MaimaiImage.fonts[attr.fontName]!!,
    attr.size.toDouble(),
    align,
    attr.xScale,
    attr.stroke,
    attr.strokeColor.hexToRGBA(),
    ellipsizeWidth
)
fun Bitmap.randomSlice(size: Int = 66) =
    sliceWithSize((0..width - size).random(), (0..height - size).random(), size, size).extract()