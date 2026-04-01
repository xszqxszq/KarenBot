package xyz.xszq.bot.component

import korlibs.io.util.isDigit
import korlibs.io.util.toStringDecimal
import korlibs.math.toIntFloor
import org.jetbrains.skia.Image
import xyz.xszq.bot.Filter
import xyz.xszq.bot.Maimai
import xyz.xszq.bot.Query
import xyz.xszq.bot.api.MaimaiAPI
import xyz.xszq.bot.image.dom.Div
import xyz.xszq.bot.image.dom.Element
import xyz.xszq.bot.image.dom.Img
import xyz.xszq.bot.image.parse.StyleParser.rgbColor
import xyz.xszq.bot.image.parse.StyleParser.rgba
import xyz.xszq.bot.image.style.BackgroundPosition
import xyz.xszq.bot.image.style.BackgroundSize
import xyz.xszq.bot.image.style.ObjectFit
import xyz.xszq.bot.image.style.Spacing
import xyz.xszq.bot.image.template.Template
import xyz.xszq.bot.image.template.TemplateManager
import xyz.xszq.bot.music.*
import xyz.xszq.bot.pagination
import xyz.xszq.bot.payload.LocalCourseInfo
import kotlin.math.min

/**
 * Image Layer for Maimai Plugin.
 */
class MaimaiImage(
    val maimai: Maimai
) {
    lateinit var templateManager: TemplateManager

    suspend fun init() {
        templateManager = TemplateManager("./data/maimai/")
    }

    /**
     * Load Images to memory.
     */
    suspend fun loadImage() {
        templateManager.init()
    }

    /**
     * Generate a single Music Info for Course.
     * @param chart The chart to render.
     * @param record The record to display.
     */
    fun Template.courseMusic(
        chart: ChartInfo,
        achievement: Int,
        life: Int,
        damage: Int
    ) = this["music"]!!.modify {
        image("result") {
            if (life <= 0)
                src = "result_2.png"
        }
        div("music-div") {
            background = "base_${chart.difficulty.name}.png"
            image("cover") {
                src = "../../covers/${chart.music.resourceId}.png"
            }
            div("info") {
                div("header") {
                    image("type") {
                        src = "type_${chart.music.type.value}.png"
                    }
                    div("title-div") {
                        text("title") {
                            text = chart.music.name
                        }
                    }
                }
                div("level-div") {
                    image("level") {
                        src = "level_${chart.difficulty.value}_level.png"
                    }
                    div("value") {
                        val digits = chart.level.filter { it.isDigit() }
                        digits.forEachIndexed { index, value ->
                            add(Img(src="level_${chart.difficulty.value}_$value.png").apply {
                                style.margin = Spacing(
                                    0f, 0f, 0f,
                                    if (index != 0) -12f else 0f
                                )
                                style.objectFit = ObjectFit.NONE
                            })
                        }
                    }
                    image("plus") {
                        src = "level_${if (chart.level.endsWith("+")) chart.difficulty.value else 5}_plus.png"
                    }
                }
                div("achievement") {
                    val part0 = (achievement / 10000).toString().padStart(3, ' ')
                    val part1 = (achievement % 10000).toString().padStart(4, '0')
                    val type = when {
                        achievement < 800000 -> 1
                        achievement < 970000 -> 2
                        else -> 3
                    }

                    part0.forEachIndexed { index, char ->
                        image("acc-0-$index") {
                            src = if (char == ' ')
                                "score_1_none.png"
                            else
                                "score_1_${type}_${char}.png"
                        }
                    }
                    image("acc-dot") {
                        src = "score_3_${type}_dot.png"
                    }
                    part1.forEachIndexed { index, char ->
                        image("acc-1-$index") {
                            src = "score_3_${type}_${char}.png"
                        }
                    }
                    image("acc-percent") {
                        src = "percent_2_${type}.png"
                    }
                }
                div("damage-div") {
                    text("damage") {
                        text = "-${damage}"
                    }
                }
            }
        }
    }
    /**
     * Template Course Score generation.
     */
    suspend fun templateCourse(
        course: LocalCourseInfo,
        scores: List<Pair<ChartInfo, Record?>>
    ): Image {
        var life = course.life
        val damages = scores.mapIndexed { index, (chart, score) ->
            if (score ?.comboStatus ?.isAP() == true)
                0
            else
                calcMinDamage(chart, score?.achievement ?: 0, course)
        }
        val remains = damages.mapIndexed { index, damage ->
            if (life > 0 && index != 0)
                life += course.recover
            life -= damage
            if (life <= 0) {
                life = 0
            }
            life
        }
        val theme = templateManager["course"]!!
        val main = theme["main"]!!.modify {
            val realId = course.id % 10000
            background = when {
                realId <= 1010 -> "background_1.png"
                realId <= 1112 -> "background_2.png"
                else -> "background_3.png"
            }
            div("upper/info") {
                div("status") {
                    if (life <= 0)
                        background = "final_2.png"
                    image("course") {
                        src = "title_${course.id}.png"
                    }
                }
                div("life") {
                    val type = when {
                        life >= 100 -> 1
                        life >= 10 -> 2
                        life >= 1 -> 3
                        else -> 4
                    }
                    background = "life_$type.png"
                    life.toString().forEach { value ->
                        add(Img(src = "life_${type}_${value}.png").apply {
                            style.objectFit = ObjectFit.NONE
                        })
                    }
                }
                div("detail/header/life") {
                    text("value") {
                        text = course.life.toString()
                    }
                }
                div("detail/achievement") {
                    val total = scores.sumOf { it.second ?.achievement ?: 0 }
                    val type = when {
                        total < 800000 * scores.size -> 1
                        total < 970000 * scores.size -> 2
                        else -> 3
                    }

                    val part0 = (total / 10000).toString().padStart(3, ' ')
                    val part1 = (total % 10000).toString().padStart(4, '0')
                    part0.forEachIndexed { index, char ->
                        image("acc-0-$index") {
                            src = if (char == ' ')
                                "score_2_none.png"
                            else
                                "score_2_${type}_${char}.png"
                        }
                    }
                    image("acc-dot") {
                        src = "score_1_${type}_dot.png"
                    }
                    part1.forEachIndexed { index, char ->
                        image("acc-1-$index") {
                            src = "score_1_${type}_${char}.png"
                        }
                    }
                    image("acc-percent") {
                        src = "percent_1_${type}.png"
                    }
                }
                div("detail/damage") {
                    div("great") {
                        text("value") {
                            text = "-${course.damage.great}"
                        }
                    }
                    div("good") {
                        text("value") {
                            text = "-${course.damage.good}"
                        }
                    }
                    div("miss") {
                        text("value") {
                            text = "-${course.damage.miss}"
                        }
                    }
                }
                text("detail/heal-div/heal/value") {
                    text = "+${course.recover}"
                }
            }
            div("upper/musics") {
                scores.forEachIndexed { index, (chart, record) ->
                    add(theme.courseMusic(
                        chart, record ?.achievement ?: 0, remains[index], damages[index]
                    ))
                }
            }
        }
        return theme.render(main)
    }
    companion object {
        /**
         * Get color for different difficulties.
         * @param difficulty Chart Difficulty.
         */
        fun levelChartColor(
            difficulty: MusicDifficulty
        ) = when (difficulty) {
            MusicDifficulty.Basic -> "#45c124"
            MusicDifficulty.Advanced -> "#f8b709"
            MusicDifficulty.Expert -> "#ff5a66"
            MusicDifficulty.Master -> "#9f51dc"
            MusicDifficulty.ReMaster -> "#dbaaff"
            MusicDifficulty.Utage -> "#ff6ffd"
        }

        fun calcMinDamage(
            chart: ChartInfo,
            achievement: Int,
            course: LocalCourseInfo
        ): Int {
            val totalBase = chart.notes.tap + chart.notes.touch +
                    2 * chart.notes.hold + 3 * chart.notes.slide +
                    5 * chart.notes.`break`
            val base = 100000.0 / totalBase

            val minus = 1010000 - achievement
            val amount = minus / base

            val greats = (amount / 2).toIntFloor()
            val goods = (amount / 5).toIntFloor()
            val misses = (amount / 10).toIntFloor()

            val damages = mutableListOf<Int>()
            if (course.damage.great != 0)
                damages.add(course.damage.great * greats)
            if (course.damage.good != 0)
                damages.add(course.damage.good * goods)
            if (course.damage.miss != 0)
                damages.add(course.damage.miss * misses)
            return damages.minOrNull() ?: 0
        }
    }

    fun headerNew(
        header: Div,
        response: Response,
        old: Boolean
    ) = header.run {
        background = "../../plates/${response.plate}.png"
        div("info/rating") {
            val color = if (old) Rating.colorOld(response.rating) else Rating.color(response.rating)
            background = "rating_base_$color.png"
            response.rating.toString().forEach { digit ->
                add(Img(src = "rating_$digit.png").apply {
                    style.objectFit = ObjectFit.NONE
                })
            }
        }
        text("name") {
            text = response.name
        }
        image("avatar") {
            src = "../../avatars/${response.icon}.png"
        }
        image("course") {
            src = "dani_${response.course}.png"
        }
    }

    fun Template.score(
        index: Int,
        record: Record,
        isFitLevelValues: Boolean = false
    ) = this["music"]!!.modify {
        background = "base_${record.chart.difficulty.name}.png"
        div("cover") {
            background = "../../covers/${record.music.resourceId}_s.jpg"
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
            val levelValue = if (isFitLevelValues)
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
    suspend fun templateRatingNew(
        response: RatingResponse,
        noB15: Boolean = false,
        old: Int = 35,
        new: Int = 15,
        backend: MaimaiAPI ?= null,
        isFitLevelValues: Boolean = false
    ): org.jetbrains.skia.Image {
        val template = templateManager["rating"]!!

        val main = template["main"]!!.modify {
            if (noB15)
                div("upper") {
                    image("icon-b15") {
                        src = "icon_no_b15.png"
                    }
                }
            div("upper/header") {
                headerNew(this@div, response, old == 25)
                text("name-title") {
                    val oldRating = response.ratingList.take(old).sumOf { it.rating }
                    val newRating = response.newRatingList.take(new).sumOf { it.rating }

                    text = buildString {
                        backend ?.let {
                            when (backend.name) {
                                "diving-fish" -> append("[水鱼] ")
                                "lxns" -> append("[落雪] ")
                            }
                        }
                        if (old == 25)
                            append("$oldRating + $newRating + ${response.rating - oldRating - newRating} = ${response.rating}")
                        else
                            append("$oldRating + $newRating = ${oldRating + newRating}")
                    }
                }
            }

            div("upper/best-35") {
                response.ratingList.take(old).forEachIndexed { index, record ->
                    add(template.score(index, record, isFitLevelValues))
                }
                repeat(old - response.ratingList.take(old).size) {
                    add(template["music"]!!)
                }
            }
            div("upper/best-15") {
                response.newRatingList.take(new).forEachIndexed { index, record ->
                    add(template.score(index, record, isFitLevelValues))
                }
                repeat(new - response.newRatingList.take(new).size) {
                    add(template["music"]!!)
                }
            }
        }

        return template.render(main)
    }

    suspend fun templateBest50New(
        response: RecordsResponse,
        nowVersion: GameVersion,
        backend: MaimaiAPI,
        isAllRequired: Boolean,
        isFitLevelValues: Boolean = false,
        lambda: suspend List<Record>.() -> List<Record>?
    ): org.jetbrains.skia.Image? {
        val records = lambda(response.records) ?: return null
        if (isFitLevelValues)
            records.forEach {
                it.rating = Rating.calc(it.chart.fitLevelValue, it.achievement)
            }
        val b50 = records.take(50)
        val noB15 = isAllRequired || if (nowVersion == maimai.local.newestVersion)
            records.filter { it.music.version == nowVersion }.size < 15 || records.none { it.music.version != nowVersion }
        else
            false
        val b35 = if (!noB15) records.filter { it.music.version != nowVersion }.take(35) else b50.take(35)
        val b15 = if (!noB15) records.filter { it.music.version == nowVersion }.take(15) else b50.subList(min(35, b35.size), b50.size)
        return templateRatingNew(RatingResponse(
            name = response.name,
            rating = b35.sumOf { it.rating } + b15.sumOf { it.rating },
            course = response.course,
            icon = response.icon,
            plate = response.plate,
            ratingList = b35,
            newRatingList = b15
        ), noB15, backend = backend, isFitLevelValues = isFitLevelValues)
    }

    suspend fun templateBest40New(
        response: RecordsResponse,
        nowVersion: GameVersion,
        backend: MaimaiAPI,
        isAllRequired: Boolean,
        isFitLevelValues: Boolean = false,
        lambda: suspend List<Record>.() -> List<Record>?
    ): org.jetbrains.skia.Image? {
        val records = lambda(response.records) ?: return null
        records.forEach {
            if (isFitLevelValues)
                it.rating = Rating.calcOld(it.chart.fitLevelValue, it.achievement)
            else
                it.rating = Rating.calcOld(it.chart, it.achievement)
        }
        val b40 = records.take(40)
        val noB15 = isAllRequired || if (nowVersion == maimai.local.newestVersion)
            records.filter { it.music.version == nowVersion }.size < 15 || records.none { it.music.version != nowVersion }
        else
            false
        val b25 = if (!noB15) records.filter { it.music.version != nowVersion }.take(25) else b40.take(25)
        val b15 = if (!noB15) records.filter { it.music.version == nowVersion }.take(15) else b40.subList(min(25, b25.size), b40.size)
        return templateRatingNew(RatingResponse(
            name = response.name,
            rating = b25.sumOf { it.rating } + b15.sumOf { it.rating } + Rating.courseOld(response.course),
            course = response.course,
            icon = response.icon,
            plate = response.plate,
            ratingList = b25,
            newRatingList = b15
        ), noB15 = noB15, old = 25, new = 15, backend = backend, isFitLevelValues = isFitLevelValues)
    }

    suspend fun templateScoreListNew(
        response: RecordsResponse,
        name: String,
        page: Int,
        all: Boolean = false,
        isFitLevelValues: Boolean = false,
        lambda: suspend List<Record>.() -> List<Record>?
    ): Triple<org.jetbrains.skia.Image, Int, Int>? {
        var pageSize = 50
        val (records, actualPage, totalPages) = lambda(response.records) ?.let { filtered ->
            val records = filtered.sortedBy { -it.achievement }
            if (all)
                Triple(records, 1, 1).also { pageSize = records.size }
            else
                records.pagination(page, pageSize)
        } ?: return null

        val template = templateManager["rating"]!!
        val main = template["main"]!!.modify {
            if (all) {
                style.backgroundSize = BackgroundSize.COVER
                style.backgroundPosition = BackgroundPosition.TOP_CENTER
                style.minHeight = style.height
                style.height = null
            }
            div("upper/header") {
                headerNew(this@div, response, false)
                text("name-title") {
                    text = if (all)
                        "${name}分数列表"
                    else
                        "${name}分数列表，第 $actualPage 页 (共 $totalPages 页)"
                }
            }

            div("upper/best-35") {
                records.forEachIndexed { index, record ->
                    add(template.score(index, record, isFitLevelValues))
                }
                repeat(pageSize - records.size) {
                    add(template["music"]!!)
                }
            }

            image("icon-b15") {
                src = ""
            }
        }
        return Triple(template.render(main), actualPage, totalPages)
    }


    fun headerInfoScoreNew(
        header: Div,
        music: MusicInfo
    ) = header.run {
        image("cover") {
            src = "../../covers/${music.resourceId}.png"
        }
        div("right") {
            div("top") {
                image("type") {
                    src = "type_${music.type.value}.png"
                }
                text("id") {
                    text = "ID ${music.id}"
                }
            }
            div("title-container") {
                text("artist") {
                    text = music.artist
                }
                text("title") {
                    text = music.name
                }
            }
            div("bottom") {
                image("genre") {
                    src = "genre_${music.genre.id}.png"
                }
                image("version") {
                    src = "version_${music.version.version}.png"
                }
                text("bpm") {
                    text = "BPM ${music.bpm}"
                }
            }
        }
    }

    fun Template.infoScore(
        chart: ChartInfo,
        record: Record?
    ): Element = this["chart"]!!.modify {
        background = "base_${chart.difficulty.name}.png"
        div("level-container") {
            val nowColor = levelChartColor(chart.difficulty)
            text("icon") {
                style.textColor = nowColor.rgbColor()!!
            }
            text("level") {
                style.textColor = nowColor.rgbColor()!!
                if (chart.levelValue != 0.0)
                    text = chart.levelValue.toString()
            }
        }
        record ?.let {
            div("fixed-acc") {
                text("achievement") {
                    text = Rate.toString(record.achievement)
                }
            }
            div("fixed-rank") {
                image("rank") {
                    src = "rank_${record.rate}.png"
                }
            }
            image("combo") {
                src = "icon_${record.comboStatus.value}.png"
            }
            image("sync") {
                src = "icon_${record.syncStatus.value}.png"
            }
            div("dxstar-container") {
                text("value") {
                    text = "${record.deluxeScore}/${chart.maxDeluxeScore}"
                }
                image("icon") {
                    val stars = DeluxeScore.stars(record.deluxeScore, record.chart.maxDeluxeScore)
                    src = "icon_dxstar_$stars.png"
                }
            }
        }
        div("note-designer") {
            text("designer") {
                text = chart.notesDesigner
            }
        }
        if (chart.levelValue == 0.0) {
            div("fixed-acc") {
                text("achievement") {
                    text = "无此难度"
                }
            }
        }
    }

    suspend fun templateInfoScoreNew(
        music: MusicInfo,
        records: List<Record>? = null
    ): org.jetbrains.skia.Image {
        val template = templateManager["score"]!!
        val main = template["main"]!!.modify {
            div("upper") {
                div("header-cover") header@ {
                    background = "../../covers/${music.resourceId}.png"
                    headerInfoScoreNew(this@header, music)
                }
                if (music.genre == MusicGenre.Utage)
                    music.charts.first().let { chart ->
                        add(template.infoScore(chart, records?.firstOrNull()))
                    }
                else
                    music.charts.forEach { chart ->
                        val record = records?.firstOrNull { it.chart.difficulty == chart.difficulty }
                        add(template.infoScore(chart, record))
                    }
                if (music.genre != MusicGenre.Utage && music.charts.none { it.difficulty == MusicDifficulty.ReMaster }) {
                    add(template.infoScore(music.fakeReMaster, null))
                }
            }
        }
        return template.render(main)
    }


    fun Template.levelChart(
        chart: ChartInfo,
        requiresType: RequiresType = RequiresType.Achievement,
        record: Record? = null,
    ) = this["music"]!!.modify {
        style.backgroundColor = levelChartColor(chart.difficulty).rgbColor()
        div("cover") {
            val id = chart.music.id.toString()
            background = "../../covers/${chart.music.resourceId}_s.jpg"
            div("overlay") {
                div("info") {
                    image("type") {
                        src = "type_${chart.music.type.value}.png"
                    }
                    div("id-container") {
                        style.backgroundColor = levelChartColor(chart.difficulty).rgbColor()
                        style.width = (25 + (id.length - 3) * 5).toFloat()
                        text("id") {
                            text = id
                        }
                    }
                }

                record ?.let {
                    when (requiresType) {
                        RequiresType.Achievement -> {
                            if (record.achievement >= 800000) {
                                "rank_${record.rate}.png"
                            } else {
                                null
                            }
                        }
                        RequiresType.Combo -> {
                            if (record.comboStatus.isFC()) {
                                "icon_${record.comboStatus.value}.png"
                            } else {
                                null
                            }
                        }
                        RequiresType.Sync -> {
                            if (record.syncStatus.isFS()) {
                                "icon_${record.syncStatus.value}.png"
                            } else {
                                null
                            }
                        }
                    } ?.let { icon ->
                        style.backgroundColor = rgba(0, 0, 0, 0.5f)
                        image("score") {
                            src = icon
                        }
                    }
                }
            }

        }
    }

    suspend fun templateLevelNew(
        filters: List<Filter>?,
        charts: List<ChartInfo>,
        title: String,
        detailed: Boolean,
        requiresType: RequiresType = RequiresType.Achievement,
        records: List<Record>? = null,
        isFitLevelValues: Boolean = false,
    ): org.jetbrains.skia.Image {
        val groups = if (detailed) {
            charts.groupBy {
                if (isFitLevelValues)
                    it.fitLevelValue.toStringDecimal(1)
                else
                    it.levelValue.toString()
            }
        } else {
            charts.groupBy {
                if (isFitLevelValues)
                    Level.toLevel(it.fitLevelValue)
                else
                    it.level
            }
        }.toSortedMap(Level.comparator).toList().reversed()

        val matched = charts.associateWith { chart ->
            records?.firstOrNull {
                it.music.id == chart.music.id && it.chart.difficulty == chart.difficulty
            }
        }
        val filtered = Query.filterRecords(filters, records ?: emptyList())
        val completed = charts.associateWith { chart ->
            filtered ?.firstOrNull {
                it.music.id == chart.music.id && it.chart.difficulty == chart.difficulty
            }
        }

        val template = templateManager["level"]!!
        val main = template["main"]!!.modify {
            div("upper/header") {
                text("title") {
                    text = title
                }
                image("all") {
                    if (completed.values.any { it == null })
                        return@image
                    src = when (requiresType) {
                        RequiresType.Achievement -> {
                            val min = completed.values
                                .filterNotNull()
                                .minBy { it.achievement }
                            when {
                                min.achievement >= 970000 -> min.rate
                                min.achievement >= 800000 -> "clear"
                                else -> null
                            }
                        }
                        RequiresType.Combo -> {
                            val min = completed.values
                                .filterNotNull()
                                .filter { it.comboStatus.isFC() }
                                .minByOrNull { it.comboStatus }
                            min?.comboStatus?.value
                        }
                        RequiresType.Sync -> {
                            val min = completed.values
                                .filterNotNull()
                                .filter { it.syncStatus.isFS() }
                                .minByOrNull { it.syncStatus }
                            min?.syncStatus?.value
                        }
                    } ?.let {
                        "all_$it.png"
                    }
                }
            }

            div("upper/list") {
                groups.forEach { (level, charts) -> add(template["group"]!!.modify {
                    text("level") {
                        text = level
                    }
                    div("musics") {
                        charts.forEach { chart ->
                            add(template.levelChart(chart, requiresType, matched[chart]))
                        }
                    }
                })}
            }
        }
        return template.render(main)
    }
}