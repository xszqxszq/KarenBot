package xyz.xszq.bot.maimai

import com.soywiz.kds.atomic.kdsFreeze
import com.soywiz.klock.measureTime
import com.soywiz.klock.measureTimeWithResult
import com.soywiz.klock.milliseconds
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.effect.BitmapEffect
import com.soywiz.korim.bitmap.effect.applyEffect
import com.soywiz.korim.bitmap.sliceWithSize
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.font.DefaultTtfFont
import com.soywiz.korim.font.TtfFont
import com.soywiz.korim.font.TtfNativeSystemFontProvider
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

fun Context2d.drawText(text: String, attr: ItemPosition, color: RGBA = Colors.BLACK,
                       align: TextAlignment = TextAlignment.LEFT) {
    if (attr.fontName.isNotBlank())
        this.font = MaimaiImage.fonts[attr.fontName]
    this.fontSize = attr.size.toDouble()
    this.alignment = align
    this.fillStyle = createColor(color)
    // TODO: Submit issue to korim to fix alignment, this is only a TEMPORARY solution
    val offsetx = if (this.alignment.horizontal == HorizontalAlign.RIGHT) -1 * (attr.size - 1) * (text.length / 2) else 0
    fillText(text, attr.x.toDouble() + offsetx, attr.y.toDouble())
}
fun Context2d.drawTextRelative(text: String, x: Int, y: Int, attr: ItemPosition,
                               color: RGBA = Colors.BLACK, align: TextAlignment = TextAlignment.LEFT) {
    this.font = MaimaiImage.fonts[attr.fontName]
    this.fontSize = attr.size.toDouble()
    this.alignment = align
    this.fillStyle = createColor(color)
    // TODO: Submit issue to korim to fix alignment, this is only a TEMPORARY solution
    val offsetx = if (this.alignment.horizontal == HorizontalAlign.RIGHT) -1 * (attr.size - 1) * (text.length / 2) else 0
    fillText(text, x + attr.x.toDouble() + offsetx, y + attr.y.toDouble())
}

fun Bitmap32.blurFixedSize(radius: Int) = applyEffect(BitmapEffect(radius))
    .removeAlpha().sliceWithSize(radius, radius, width, height).extract()
fun Bitmap32.brightness(ratio: Float = 0.6f): Bitmap32 {
    if (ratio > 1f || ratio < -1f)
        throw IllegalArgumentException("Ratio must be in [-1, 1]")
    val real = ratio / 2f + 0.5f
    updateColors {
        it.times(RGBA.float(real, real, real, 1f))
    }
    return this
}
fun Bitmap32.removeAlpha(): Bitmap32 {
    forEach { _, x, y ->
        this[x, y] = RGBA(this[x, y].r, this[x, y].g, this[x, y].b, 255)
    }
    return this
}
fun Bitmap.randomSlice(size: Int = 66) =
    sliceWithSize((0..width - size).random(), (0..height - size).random(), size, size).extract()