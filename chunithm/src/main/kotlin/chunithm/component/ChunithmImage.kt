package xyz.xszq.bot.chunithm.component

import korlibs.io.file.baseNameWithoutExtension
import korlibs.io.file.std.localCurrentDirVfs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.skia.*
import xyz.xszq.bot.chunithm.component.image.templates.RatingTemplate
import xyz.xszq.bot.chunithm.music.ChartInfo
import xyz.xszq.bot.chunithm.music.MusicDifficulty
import xyz.xszq.shinobu.parse.StyleParser.rgbColor
import xyz.xszq.shinobu.template.TemplateManager

class ChunithmImage(
    val chunithmData: ChunithmData,
    val resourcePath: String = "../.."
) {
    lateinit var manager: TemplateManager

    lateinit var rating: RatingTemplate

    fun init() {
        manager = TemplateManager("./data/chunithm/")

        rating = RatingTemplate(manager, resourcePath, chunithmData.newestVersion)
    }
    /**
     * 载入模板
     */
    fun load(scope: CoroutineScope) {
        manager.init()
        scope.launch {
            generateThumb()
        }
    }

    private suspend fun generateThumb() = coroutineScope {
        val covers = localCurrentDirVfs["./data/chunithm/covers"]
        covers.listSimple().filter {
            it.baseNameWithoutExtension.toIntOrNull() != null
        }.forEach { cover ->
            val id = cover.baseNameWithoutExtension.toInt()
            val small = covers["${id}_s.jpg"]
            if (!small.exists())
                launch {
                    Image.makeFromEncoded(cover.readBytes()).use { original ->
                        Surface.makeRasterN32Premul(THUMB_SIZE, THUMB_SIZE).use { surface ->
                            surface.canvas.drawImageRect(
                                original,
                                Rect.makeWH(original.width.toFloat(), original.height.toFloat()),
                                Rect.makeWH(THUMB_SIZE.toFloat(), THUMB_SIZE.toFloat()),
                                SamplingMode.CATMULL_ROM,
                                null,
                                true
                            )
                            surface.makeImageSnapshot().use { snapshot ->
                                snapshot.encodeToData(EncodedImageFormat.JPEG, 85).use {
                                    small.writeBytes(it!!.bytes)
                                }
                            }
                        }
                    }
                }
        }
    }

    companion object {
        /**
         * 获得难度对应的颜色
         */
        fun ChartInfo.color() = when (difficulty) {
            MusicDifficulty.Basic -> "#029c73"
            MusicDifficulty.Advanced -> "#ee7508"
            MusicDifficulty.Expert -> "#e32b2c"
            MusicDifficulty.Master -> "#7e18ca"
            MusicDifficulty.Ultima -> "#131413"
            MusicDifficulty.WorldsEnd -> "#f4e819"
        }.rgbColor()!!
        const val THUMB_SIZE = 54
    }
}