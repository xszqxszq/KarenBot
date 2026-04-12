package xyz.xszq.bot.maimai.component.image.templates

import korlibs.io.util.toStringDecimal
import org.jetbrains.skia.Image
import xyz.xszq.bot.image.dom.Div
import xyz.xszq.bot.image.dom.Img
import xyz.xszq.bot.image.dom.Img.Companion.noScale
import xyz.xszq.bot.image.parse.StyleParser.rgbColor
import xyz.xszq.bot.image.style.BackgroundPosition
import xyz.xszq.bot.image.style.BackgroundSize
import xyz.xszq.bot.image.template.Template
import xyz.xszq.bot.image.template.TemplateManager
import xyz.xszq.bot.maimai.component.image.FilterParams
import xyz.xszq.bot.maimai.component.image.RatingRenderParams
import xyz.xszq.bot.maimai.music.*
import xyz.xszq.bot.pagination
import kotlin.math.min

class RatingTemplate(
    private val manager: TemplateManager,
    private val resourcePath: String,
    private val newestVersion: GameVersion
) {
    fun title(
        backend: String,
        oldRating: Int,
        newRating: Int,
        courseRating: Int?,
        rating: Int
    ) = buildString {
        append("[${backend}] ")
        append("$oldRating")
        append(" + $newRating")
        courseRating ?.let {
            append(" + $courseRating")
        }
        append(" = $rating")
    }
    /**
     * 最佳成绩列表模板
     * @param total 取最佳多少项成绩进行统计
     * @param info 查询信息
     * @param backend 数据源名称
     */
    suspend fun bests(
        total: Int,
        info: RatingResponse,
        backend: String
    ): Image {
        val newCount = 15
        val oldCount = total - newCount
        val isOldRating = total < 50

        if (isOldRating) {
            info.oldRatingList.forEach { record ->
                record.rating = Rating.calcOld(record.chart, record.achievement)
            }
            info.newRatingList.forEach { record ->
                record.rating = Rating.calcOld(record.chart, record.achievement)
            }
        }

        val oldRating = info.oldRatingList.sumOf { it.rating }
        val newRating = info.newRatingList.sumOf { it.rating }
        var rating = oldRating + newRating
        val courseRating = if (isOldRating) Rating.courseOld(info.player.course) else null
        courseRating ?.let { rating += it }

        val title = title(backend, oldRating, newRating, courseRating, rating)

        return template(
            RatingRenderParams(
                nickname = info.player.nickname,
                rating = rating,
                ratingColor =
                    if (isOldRating) Rating.colorOld(rating)
                    else Rating.color(rating),
                course = info.player.course,
                avatar = info.settings ?.avatar ?: 101,
                plate = info.settings ?.plate ?: 11,
                filter = null,
                title = title,
                oldCount = oldCount,
                newCount = newCount,
                isNewDisabled = false,
                oldRecords = info.oldRatingList.take(oldCount),
                newRecords = info.newRatingList.take(newCount),
            )
        )
    }
    /**
     * 随心配最佳成绩列表模板
     * @param total 取最佳多少项成绩进行统计
     * @param player 玩家信息
     * @param settings 玩家设置
     * @param allRecords 所有成绩
     * @param filterParams 条件过滤参数
     * @param api 数据源名称
     */
    suspend fun comboBests(
        total: Int,
        player: PlayerInfo,
        settings: PlayerSettings ?= null,
        allRecords: List<Record>,
        filterParams: FilterParams ?= null,
        api: String
    ): Image {
        val newCount = 15
        val oldCount = total - newCount
        val isOldRating = total < 50

        when {
            isOldRating && filterParams ?.isFitLevelValue == false -> allRecords.forEach { record ->
                record.rating = Rating.calcOld(record.chart, record.achievement)
            }
            !isOldRating && filterParams ?.isFitLevelValue == true -> allRecords.forEach { record ->
                record.rating = Rating.calc(record.chart.fitLevelValue, record.achievement)
            }
            isOldRating && filterParams ?.isFitLevelValue == true -> allRecords.forEach { record ->
                record.rating = Rating.calcOld(record.chart.fitLevelValue, record.achievement)
            }
        }

        val bests = allRecords.take(total)
        val isNewDisabled = when {
            filterParams == null -> false
            filterParams.isAllRequired -> true
            filterParams.newestVersion == newestVersion ->
                allRecords.filter { it.music.version == filterParams.newestVersion }.size < newCount
            allRecords.none { it.music.version != filterParams.newestVersion } -> true
            else -> false
        }

        val oldRecords =
            if (isNewDisabled) bests.take(oldCount)
            else allRecords.filter { it.music.version != filterParams ?.newestVersion }.take(oldCount)
        val newRecords =
            if (isNewDisabled) bests.subList(min(oldCount,oldRecords.size), bests.size)
            else allRecords.filter { it.music.version == filterParams ?.newestVersion }.take(newCount)

        val oldRating = oldRecords.sumOf { it.rating }
        val newRating = newRecords.sumOf { it.rating }
        var rating = oldRating + newRating
        val courseRating = if (isOldRating) Rating.courseOld(player.course) else null
        courseRating ?.let { rating += it }

        val title = title(api, oldRating, newRating, courseRating, rating)

        return template(
            RatingRenderParams(
                nickname = player.nickname,
                rating = rating,
                ratingColor =
                    if (isOldRating) Rating.colorOld(rating)
                    else Rating.color(rating),
                course = player.course,
                avatar = settings ?.avatar ?: 101,
                plate = settings ?.plate ?: 11,
                filter = filterParams,
                title = title,
                oldCount = oldCount,
                newCount = newCount,
                isNewDisabled = isNewDisabled,
                oldRecords = oldRecords,
                newRecords = newRecords,
            )
        )
    }

    /**
     * 最佳成绩列表模板
     * @param player 玩家信息
     * @param settings 玩家设置
     * @param allRecords 所有成绩信息
     * @param filterParams 条件过滤参数
     * @param page 查询页数
     */
    suspend fun scoreList(
        player: PlayerInfo,
        settings: PlayerSettings ?= null,
        allRecords: List<Record>,
        filterParams: FilterParams,
        page: Int
    ): Triple<Image, Int, Int> {
        val pageSize = 50

        if (filterParams.isFitLevelValue)
            allRecords.forEach { record ->
                record.rating = Rating.calc(record.chart.fitLevelValue, record.achievement)
            }

        val (records, actualPage, totalPages) = allRecords.let { filtered ->
            val records = filtered.sortedBy { -it.achievement }
            if (filterParams.isAllRequired)
                Triple(records, 1, 1)
            else
                records.pagination(page, pageSize)
        }

        val title = if (filterParams.isAllRequired)
            "${filterParams.name}分数列表"
        else
            "${filterParams.name}分数列表，第 $actualPage 页 (共 $totalPages 页)"

        return Triple(template(
            RatingRenderParams(
                nickname = player.nickname,
                rating = player.rating,
                ratingColor = Rating.color(player.rating),
                course = player.course,
                avatar = settings ?.avatar ?: 101,
                plate = settings ?.plate ?: 11,
                filter = filterParams,
                title = title,
                oldCount = 50,
                newCount = 0,
                isNewDisabled = true,
                isScoreList = true,
                oldRecords = records,
                newRecords = emptyList(),
            )
        ), actualPage, totalPages)
    }

    /**
     * 生成模板
     * @param params 渲染参数
     */
    private suspend fun template(
        params: RatingRenderParams
    ): Image {
        val template = manager["rating"]!!

        val main = template["main"]!!.modify {
            div("upper/header") {
                header(params)
                text("name-title") {
                    text = params.title
                }
            }

            div("upper/best-35") {
                params.oldRecords.forEachIndexed { index, record ->
                    add(template.score(index, record, params))
                }
                repeat(params.oldCount - params.oldRecords.size) {
                    add(template["music"]!!)
                }
            }
            div("upper/best-15") {
                params.newRecords.forEachIndexed { index, record ->
                    add(template.score(index, record, params))
                }
                repeat(params.newCount - params.newRecords.size) {
                    add(template["music"]!!)
                }
            }

            image("upper/icon-b15") {
                when {
                    params.isScoreList -> src = ""
                    params.isNewDisabled -> src = "icon_no_b15.png"
                }
            }
            if (params.isScoreList && params.filter ?.isAllRequired == true) {
                style.backgroundSize = BackgroundSize.COVER
                style.backgroundPosition = BackgroundPosition.TOP_CENTER
                style.minHeight = style.height
                style.height = null
            }
        }

        return template.render(main)
    }

    /**
     * 顶栏展示个人信息
     * @param params 渲染模板
     */
    private fun Div.header(
        params: RatingRenderParams
    ) {
        background = "$resourcePath/plates/${params.plate}.png"
        div("info/rating") {
            background = "rating_base_${params.ratingColor}.png"
            params.rating.toString().forEach { digit ->
                add(Img(src = "rating_$digit.png").noScale())
            }
        }
        text("name") {
            text = params.nickname
        }
        image("avatar") {
            src = "$resourcePath/avatars/${params.avatar}.png"
        }
        image("course") {
            src = "dani_${params.course}.png"
        }
    }

    /**
     * 展示单个成绩
     * @param index 序号
     * @param record 成绩记录
     * @param params 渲染参数
     */
    private fun Template.score(
        index: Int,
        record: Record,
        params: RatingRenderParams
    ) = this["music"]!!.modify {
        background = "base_${record.chart.difficulty.name}.png"
        div("cover") {
            background = "$resourcePath/covers/${record.music.resourceId}_s.jpg"
        }
        text("index-id") {
            text = "#${index + 1} ${record.music.id}"
        }
        text("title") {
            text = record.music.name
        }
        text("achievement-integer") {
            text = "${record.achievement / 10000}"
        }
        text("achievement-decimal") {
            text = "." + (record.achievement % 10000).toString().padStart(4, '0')
        }
        text("achievement-percent") {
            text = "%"
        }
        text("level-rating") {
            val levelValue = if (params.filter ?.isFitLevelValue == true)
                record.chart.fitLevelValue.toStringDecimal(1)
            else
                record.chart.levelValue.toStringDecimal(1)
            text = "$levelValue→${record.rating}"
            style.textColor = when (record.chart.difficulty) {
                MusicDifficulty.Basic -> "#45c124".rgbColor()
                MusicDifficulty.Advanced -> "#f8b709".rgbColor()
                MusicDifficulty.Expert -> "#ff5a66".rgbColor()
                MusicDifficulty.Master -> "#9f51dc".rgbColor()
                MusicDifficulty.ReMaster -> "#dbaaff".rgbColor()
                MusicDifficulty.Utage -> "#ff6ffd".rgbColor()
            }!!
        }
        image("type") {
            src = "type_${record.music.type.value}.png"
        }
        image("rank") {
            src = "rank_${record.rate}.png"
        }
        image("fc") {
            src = "icon_${record.comboStatus.value}.png"
        }
        image("fs") {
            src = "icon_${record.syncStatus.value}.png"
        }
        image("deluxe-star") {
            val stars = DeluxeScore.stars(record.deluxeScore, record.chart.maxDeluxeScore)
            src = "icon_dxstar_${stars}.png"
        }
    }
}