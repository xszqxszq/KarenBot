package xyz.xszq.bot.maimai.component.image

import xyz.xszq.bot.image.parse.StyleParser.rgbColor
import xyz.xszq.bot.image.template.TemplateManager
import xyz.xszq.bot.maimai.component.MaimaiData
import xyz.xszq.bot.maimai.component.image.templates.CourseTemplate
import xyz.xszq.bot.maimai.component.image.templates.LevelTemplate
import xyz.xszq.bot.maimai.component.image.templates.RatingTemplate
import xyz.xszq.bot.maimai.component.image.templates.ScoreTemplate
import xyz.xszq.bot.maimai.music.ChartInfo
import xyz.xszq.bot.maimai.music.MusicDifficulty

/**
 * 图片生成模块
 */
class MaimaiImage(
    val maimaiData: MaimaiData,
    val resourcePath: String = "../.."
) {
    lateinit var manager: TemplateManager

    lateinit var rating: RatingTemplate
    lateinit var score: ScoreTemplate
    lateinit var level: LevelTemplate
    lateinit var course: CourseTemplate

    /**
     * 初始化
     */
    fun init() {
        manager = TemplateManager("./data/maimai/")

        rating = RatingTemplate(manager, resourcePath, maimaiData.newestVersion)
        score = ScoreTemplate(manager, resourcePath)
        level = LevelTemplate(manager, resourcePath)
        course = CourseTemplate(manager, resourcePath)
    }
    /**
     * 载入模板
     */
    fun load() {
        manager.init()
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
    }
}