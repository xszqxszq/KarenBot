package xyz.xszq.bot.maimai.component.image

import korlibs.io.file.baseNameWithoutExtension
import korlibs.io.file.std.localCurrentDirVfs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.skia.*
import xyz.xszq.bot.maimai.component.MaimaiData
import xyz.xszq.bot.maimai.component.image.templates.*
import xyz.xszq.bot.maimai.music.ChartInfo
import xyz.xszq.bot.maimai.music.MusicDifficulty
import xyz.xszq.shinobu.parse.StyleParser.rgbColor
import xyz.xszq.shinobu.template.TemplateManager

/**
 * 图片生成模块
 */
class MaimaiImage(
    val maimaiData: MaimaiData,
    val resourcePath: String = "../.."
) {
    private val dataPath = "./data/maimai/"
    lateinit var manager: TemplateManager

    lateinit var rating: RatingTemplate
    lateinit var score: ScoreTemplate
    lateinit var level: LevelTemplate
    lateinit var course: CourseTemplate
    lateinit var radar: RadarTemplate

    /**
     * 初始化
     */
    fun init() {
        manager = TemplateManager(dataPath)

        rating = RatingTemplate(manager, resourcePath, maimaiData.newestVersion)
        score = ScoreTemplate(manager, resourcePath)
        level = LevelTemplate(manager, resourcePath)
        course = CourseTemplate(manager, resourcePath)
        radar = RadarTemplate(dataPath)

        radar.init()
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
        val covers = localCurrentDirVfs["./data/maimai/covers"]
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
            MusicDifficulty.Basic -> "#45c124"
            MusicDifficulty.Advanced -> "#f8b709"
            MusicDifficulty.Expert -> "#ff5a66"
            MusicDifficulty.Master -> "#9f51dc"
            MusicDifficulty.ReMaster -> "#dbaaff"
            MusicDifficulty.Utage -> "#ff6ffd"
        }.rgbColor()!!
        const val THUMB_SIZE = 72
    }
}