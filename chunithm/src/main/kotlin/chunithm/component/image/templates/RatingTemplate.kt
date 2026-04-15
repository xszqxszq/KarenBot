package xyz.xszq.bot.chunithm.component.image.templates

import korlibs.io.util.toStringDecimal
import org.jetbrains.skia.Image
import xyz.xszq.bot.chunithm.component.image.FilterParams
import xyz.xszq.bot.chunithm.component.image.RatingRenderParams
import xyz.xszq.bot.chunithm.music.*
import xyz.xszq.bot.chunithm.music.Rating.ratingFloor
import xyz.xszq.bot.pagination
import xyz.xszq.shinobu.dom.Div
import xyz.xszq.shinobu.style.BackgroundPosition
import xyz.xszq.shinobu.style.BackgroundSize
import xyz.xszq.shinobu.template.Template
import xyz.xszq.shinobu.template.TemplateManager
import kotlin.math.min

class RatingTemplate(
    private val manager: TemplateManager,
    private val resourcePath: String,
    private val newestVersion: GameVersion
) {
    fun title(
        backend: String,
        oldRating: Double,
        newRating: Double
    ) = buildString {
        append("[${backend}] ")
        append("B30: $oldRating, ")
        append("N20: $newRating")
    }
    /**
     * 最佳成绩列表模板
     * @param total 取最佳多少项成绩进行统计
     * @param info 查询信息
     * @param backend 数据源名称
     */
    suspend fun bests(
        info: RatingResponse,
        backend: String
    ): Image {
        val newCount = 20
        val oldCount = 30

        val oldRatingSum = info.oldRatingList.sumOf { it.rating }
        val newRatingSum = info.newRatingList.sumOf { it.rating }
        val ratingSum = oldRatingSum + newRatingSum
        val oldRating = (oldRatingSum / oldCount).ratingFloor()
        val newRating = (newRatingSum / newCount).ratingFloor()
        val rating = (ratingSum / (newCount + oldCount)).ratingFloor()

        val title = title(backend, oldRating, newRating)

        return template(
            RatingRenderParams(
                nickname = info.player.nickname,
                rating = rating,
                ratingColor = Rating.color(rating),
                level = info.player.level,
                avatar = info.settings?.avatar ?: 0,
                plate = info.settings?.plate ?: 140,
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
        player: PlayerInfo,
        settings: PlayerSettings?= null,
        allRecords: List<Record>,
        filterParams: FilterParams?= null,
        api: String
    ): Image {
        val newCount = 20
        val oldCount = 30

        val bests = allRecords.take(newCount + oldCount)
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

        val oldRatingSum = oldRecords.sumOf { it.rating }
        val newRatingSum = newRecords.sumOf { it.rating }
        val ratingSum = oldRatingSum + newRatingSum
        val oldRating = (oldRatingSum / oldCount).ratingFloor()
        val newRating = (newRatingSum / newCount).ratingFloor()
        val rating = (ratingSum / (newCount + oldCount)).ratingFloor()

        val title = title(api, oldRating, newRating)

        return template(
            RatingRenderParams(
                nickname = player.nickname,
                rating = rating,
                ratingColor = Rating.color(rating),
                level = player.level,
                avatar = settings ?.avatar ?: 0,
                plate = settings ?.plate ?: 140,
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
        settings: PlayerSettings?= null,
        allRecords: List<Record>,
        filterParams: FilterParams,
        page: Int
    ): Triple<Image, Int, Int> {
        val pageSize = 50

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
                level = player.level,
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

            div("upper/scores") {
                params.oldRecords.forEachIndexed { index, record ->
                    add(template.score(index, record))
                }
                repeat(params.oldCount - params.oldRecords.size) {
                    add(template["music"]!!)
                }
            }
            div("upper/scores-new") {
                params.newRecords.forEachIndexed { index, record ->
                    add(template.score(index, record))
                }
                repeat(params.newCount - params.newRecords.size) {
                    add(template["music"]!!)
                }
            }

            div("upper/category-1") {
                text("name") {
                    when {
                        params.isScoreList -> {
                            text = "分数列表"
                        }
                        params.isNewDisabled -> {
                            text = "BEST 50"
                        }
                    }
                }
            }
            div("upper/category-2") {
                when {
                    params.isScoreList -> {
                        style.backgroundImage = null
                        style.height = 0f
                    }
                    params.isNewDisabled -> {
                        style.backgroundImage = null
                        text("name") {
                            text = ""
                        }
                    }
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
        div("info/rating-line") {
            image("rating") {
                src = "rating_${params.ratingColor}.png"
            }
            div("rating-container") {
                image("rating-dot") {
                    src = "rating_${params.ratingColor}_dot.png"
                }
                val ratingString = Rating.stringWithoutDot(params.rating)
                ratingString.forEachIndexed { index, digit ->
                    image("rating-${index + 1}") {
                        src = "rating_${params.ratingColor}_${digit}.png"
                    }
                }
            }
        }
        text("level") {
            text = params.level.toString()
        }
        text("name") {
            text = params.nickname
        }
        image("avatar") {
            src = "$resourcePath/avatars/${params.avatar}.png"
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
        record: Record
    ) = this["music"]!!.modify {
        background = "base_${record.chart.difficulty.name}.png"
        div("cover") {
            background = "$resourcePath/covers/${record.music.resourceId}_s.jpg"
        }
        text("index-id") {
            text = "#${index + 1} ${record.music.id}"
        }
        text("level-rating") {
            val levelValue = record.chart.levelValue.toStringDecimal(1)
            val ratingValue = record.rating.toStringDecimal(2)
            text = "$levelValue→$ratingValue"
        }
        text("title") {
            text = record.music.name
        }
        val (acc1, acc2) = Rate.formatted(record.achievement)
        text("achievement-1") {
            text = acc1
        }
        text("achievement-2") {
            text = acc2
        }
        image("status") {
            src = when {
                record.comboStatus != ComboStatus.None -> "icon_${record.comboStatus.resourceId}.png"
                record.chainStatus != ChainStatus.None -> "icon_${record.chainStatus.resourceId}.png"
                else -> "icon_${record.clear.ifBlank { "clear" }}.png"
            }
        }
        image("rank") {
            src = "rank_${record.rate}.png"
        }
    }
}