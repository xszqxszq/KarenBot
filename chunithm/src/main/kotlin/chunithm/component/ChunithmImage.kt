package xyz.xszq.bot.chunithm.component

import korlibs.io.file.baseNameWithoutExtension
import korlibs.io.file.std.localCurrentDirVfs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.skia.*
import xyz.xszq.bot.chunithm.component.image.templates.LevelTemplate
import xyz.xszq.bot.chunithm.component.image.templates.RatingTemplate
import xyz.xszq.bot.chunithm.music.ChartInfo
import xyz.xszq.bot.chunithm.music.MusicDifficulty
import xyz.xszq.bot.util.cpuDispatcher
import xyz.xszq.shinobu.parse.StyleParser.rgbColor
import xyz.xszq.shinobu.template.TemplateManager

/**
 * 图片生成模块
 */
class ChunithmImage(
    val chunithmData: ChunithmData,
    val dataPath: String = "./data/chunithm",
    val resourcePath: String = "../.."
) {
    lateinit var manager: TemplateManager

    lateinit var rating: RatingTemplate
    lateinit var level: LevelTemplate

    /**
     * 初始化模板管理器与各图片模板
     */
    fun init() {
        manager = TemplateManager(dataPath)

        rating = RatingTemplate(manager, resourcePath, chunithmData.newestVersion)
        level = LevelTemplate(manager, resourcePath)
    }
    /**
     * 载入模板资源并生成封面缩略图
     *
     * @param scope 协程作用域
     */
    fun load(scope: CoroutineScope) {
        manager.init()
        scope.launch(cpuDispatcher) {
            generateThumb()
        }
    }

    private suspend fun generateThumb() = coroutineScope {
        val covers = localCurrentDirVfs["${dataPath}/covers"]
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
         * 谱面难度对应的颜色
         */
        fun ChartInfo.color() = when (difficulty) {
            MusicDifficulty.Basic -> "#029c73"
            MusicDifficulty.Advanced -> "#ee7508"
            MusicDifficulty.Expert -> "#e32b2c"
            MusicDifficulty.Master -> "#7e18ca"
            MusicDifficulty.Ultima -> "#131413"
            MusicDifficulty.WorldsEnd -> "#0d59ee"
        }.rgbColor()!!
        const val THUMB_SIZE = 72
    }
}