package xyz.xszq.bot.component

import korlibs.image.bitmap.Bitmap
import korlibs.io.file.std.localCurrentDirVfs
import korlibs.io.util.toStringDecimal
import kotlinx.coroutines.flow.filter
import xyz.xszq.bot.api.MaimaiAPI
import xyz.xszq.bot.music.*
import xyz.xszq.bot.pagination
import xyz.xszq.shinobu.Container
import xyz.xszq.shinobu.Image
import xyz.xszq.shinobu.Theme
import xyz.xszq.shinobu.ThemeManager
import kotlin.Boolean
import kotlin.math.min

/**
 * Image Layer for Maimai Plugin.
 */
class MaimaiImage {
    val themes = mutableMapOf<String, Theme>()

    private val themeDir = "./data/maimai/theme/"
    val manager: ThemeManager = ThemeManager(
        localCurrentDirVfs[themeDir],
        "FZLanTingHei-R-GBK"
    )

    /**
     * Load Images to memory.
     */
    suspend fun loadImage() {
        manager.themeBaseDir.list().filter { it.isDirectory() }.collect { folder ->
            val name = folder.relativePathTo(manager.themeBaseDir)!!
            themes[name] = manager.loadTheme(name)
        }
    }

    /**
     * Generate Header Info for Scores List.
     */
    suspend fun header(
        header: Container,
        response: Response,
        old: Boolean
    ) = header.run {
        background = "../../plates/${response.plate}.png"
        sub("info/rating") {
            val color = if (old) Rating.colorOld(response.rating) else Rating.color(response.rating)
            background = "rating_base_$color.png"
            response.rating.toString().forEach { digit ->
                add(Image(src = "rating_$digit.png"))
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

    /**
     * Generate a single Score.
     */
    suspend fun Theme.score(
        index: Int,
        record: Record,
        isFitLevelValues: Boolean = false
    ): Container = this["music"].modify {
        background = "base_${record.chart.difficulty.name}.png"
        sub("cover") {
            background = "covers/${record.music.resourceId}.png"
        }
        text("index-id") {
            text = "#${index + 1} ${record.music.id}"
        }
        text("title") {
            text = record.music.name
            if (this.isCharMissing())
                this.font = "FZLTHProGBK Heavy"
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
            color = when (record.chart.difficulty) {
                MusicDifficulty.Basic -> "#45c124"
                MusicDifficulty.Advanced -> "#f8b709"
                MusicDifficulty.Expert -> "#ff5a66"
                MusicDifficulty.Master -> "#9f51dc"
                MusicDifficulty.ReMaster -> "#dbaaff"
                MusicDifficulty.Utage -> "#ff6ffd"
            }
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
    /**
     * Template Rating generation.
     * @param response Rating query response.
     */
    suspend fun templateRating(
        response: RatingResponse,
        noB15: Boolean = false,
        old: Int = 35,
        new: Int = 15,
        backend: MaimaiAPI ?= null,
        isFitLevelValues: Boolean = false
    ): Bitmap {
        val theme = themes["rating"]!!
        val main = theme.main().modify {
            if (noB15)
                sub("upper") {
                    image("icon-b15") {
                        src = "icon_no_b15.png"
                    }
                }
            sub("upper/header") {
                header(this@sub, response, old == 25)
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

            sub("upper/best-35") {
                response.ratingList.take(old).forEachIndexed { index, record ->
                    add(theme.score(index, record, isFitLevelValues))
                }
                repeat(old - response.ratingList.take(old).size) {
                    add(theme["music"])
                }
            }
            sub("upper/best-15") {
                response.newRatingList.take(new).forEachIndexed { index, record ->
                    add(theme.score(index, record, isFitLevelValues))
                }
                repeat(new - response.newRatingList.take(new).size) {
                    add(theme["music"])
                }
            }
        }
        return theme.render(main)
    }

    /**
     * Template Score List generation.
     * @param response Records query response.
     * @param name Query name.
     * @param page Query page.
     * @param lambda Records Filter.
     */
    suspend fun templateScoreList(
        response: RecordsResponse,
        name: String,
        page: Int,
        all: Boolean = false,
        isFitLevelValues: Boolean = false,
        lambda: suspend List<Record>.() -> List<Record>?
    ): Triple<Bitmap, Int, Int>? {
        var pageSize = 50
        val (records, actualPage, totalPages) = lambda(response.records) ?.let { filtered ->
            val records = filtered.sortedBy { -it.achievement }
            if (all)
                Triple(records, 1, 1).also { pageSize = records.size }
            else
                records.pagination(page, pageSize)
        } ?: return null

        val theme = themes["rating"]!!
        val main = theme.main().modify {
            if (all) {
                stretch = true
                minHeight = height
                height = null
            }
            sub("upper/header") {
                header(this@sub, response, false)
                text("name-title") {
                    text = if (all)
                        "${name}分数列表"
                    else
                        "${name}分数列表，第 $actualPage 页 (共 $totalPages 页)"
                }
            }

            sub("upper/best-35") {
                records.forEachIndexed { index, record ->
                    add(theme.score(index, record, isFitLevelValues))
                }
                repeat(pageSize - records.size) {
                    add(theme["music"])
                }
            }

            image("icon-b15") {
                src = ""
            }
        }
        return Triple(theme.render(main), actualPage, totalPages)
    }

    /**
     * Template Best 50 generation.
     * @param response Records query response.
     * @param lambda Records Filter.
     */
    suspend fun templateBest50(
        response: RecordsResponse,
        noB15: Boolean,
        nowVersion: GameVersion,
        backend: MaimaiAPI,
        isFitLevelValues: Boolean = false,
        lambda: suspend List<Record>.() -> List<Record>?
    ): Bitmap? {
        val records = lambda(response.records) ?: return null
        if (isFitLevelValues)
            records.forEach {
                it.rating = Rating.calc(it.chart.fitLevelValue, it.achievement)
            }
        val b50 = records.take(50)
        val b35 = if (!noB15) records.filter { it.music.version != nowVersion }.take(35) else b50.take(35)
        val b15 = if (!noB15) records.filter { it.music.version == nowVersion }.take(15) else b50.subList(min(35, b35.size), b50.size)
        return templateRating(RatingResponse(
            name = response.name,
            rating = b35.sumOf { it.rating } + b15.sumOf { it.rating },
            course = response.course,
            icon = response.icon,
            plate = response.plate,
            ratingList = b35,
            newRatingList = b15
        ), noB15, backend = backend, isFitLevelValues = isFitLevelValues)
    }

    /**
     * Template Best 40 generation.
     * @param response Records query response.
     * @param lambda Records Filter.
     */
    suspend fun templateBest40(
        response: RecordsResponse,
        noB15: Boolean,
        nowVersion: GameVersion,
        backend: MaimaiAPI,
        isFitLevelValues: Boolean = false,
        lambda: suspend List<Record>.() -> List<Record>?
    ): Bitmap? {
        val records = lambda(response.records) ?: return null
        records.forEach {
            if (isFitLevelValues)
                it.rating = Rating.calcOld(it.chart.fitLevelValue, it.achievement)
            else
                it.rating = Rating.calcOld(it.chart, it.achievement)
        }
        val b40 = records.take(40)
        val b25 = if (!noB15) records.filter { it.music.version != nowVersion }.take(25) else b40.take(25)
        val b15 = if (!noB15) records.filter { it.music.version == nowVersion }.take(15) else b40.subList(min(25, b25.size), b40.size)
        return templateRating(RatingResponse(
            name = response.name,
            rating = b25.sumOf { it.rating } + b15.sumOf { it.rating } + Rating.courseOld(response.course),
            course = response.course,
            icon = response.icon,
            plate = response.plate,
            ratingList = b25,
            newRatingList = b15
        ), noB15 = noB15, old = 25, new = 15, backend = backend, isFitLevelValues = isFitLevelValues)
    }

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

    /**
     * Generate a single Chart Info for Level List.
     * @param chart The chart to render.
     * @param requiresType The icon to display.
     * @param record The record to display.
     */
    suspend fun Theme.levelChart(
        chart: ChartInfo,
        requiresType: RequiresType = RequiresType.Achievement,
        record: Record? = null,
    ) = this["music"].modify {
        color = levelChartColor(chart.difficulty)
        sub("cover") {
            val id = chart.music.id.toString()
            background = "covers/${chart.music.resourceId}.png"
            sub("info") {
                image("type") {
                    src = "type_${chart.music.type.value}.png"
                }
                sub("id-container") {
                    color = levelChartColor(chart.difficulty)
                    width = 25 + (id.length - 3) * 5
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
                    color = "#000000"
                    colorOpacity = 0.5
                    image("score") {
                        src = icon
                    }
                }
            }
        }
    }

    /**
     * Template Level charts generation.
     */
    suspend fun templateLevel(
        charts: List<ChartInfo>,
        title: String,
        detailed: Boolean,
        requiresType: RequiresType = RequiresType.Achievement,
        records: List<Record>? = null,
        isFitLevelValues: Boolean = false,
    ): Bitmap {
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

        val theme = themes["level"]!!
        val main = theme.main().modify {
            sub("upper/header") {
                text("title") {
                    text = title
                }
                image("all") {
                    if (matched.values.any { it == null })
                        return@image
                    src = when (requiresType) {
                        RequiresType.Achievement -> {
                            val min = matched.values
                                .filterNotNull()
                                .minBy { it.achievement }
                            when {
                                min.achievement >= 970000 -> min.rate
                                min.achievement >= 800000 -> "clear"
                                else -> null
                            }
                        }
                        RequiresType.Combo -> {
                            val min = matched.values
                                .filterNotNull()
                                .filter { it.comboStatus.isFC() }
                                .minByOrNull { it.comboStatus }
                            min?.comboStatus?.value
                        }
                        RequiresType.Sync -> {
                            val min = matched.values
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

            sub("upper/list") {
                groups.forEach { (level, charts) -> add(theme["group"].modify {
                    text("level") {
                        text = level
                    }
                    sub("musics") {
                        charts.forEach { chart ->
                            add(theme.levelChart(chart, requiresType, matched[chart]))
                        }
                    }
                })}
            }
        }
        return theme.render(main)
    }

    suspend fun headerInfo(
        header: Container,
        music: MusicInfo
    ) = header.run {
        image("cover") {
            src = "../../covers/${music.resourceId}.png"
        }
        sub("right") {
            sub("top") {
                image("type") {
                    src = "type_${music.type.value}.png"
                }
                text("id") {
                    text = "ID ${music.id}"
                }
            }
            sub("title-container") {
                text("artist") {
                    text = music.artist
                    if (this.isCharMissing())
                        this.font = "FZLTHProGBK Heavy"
                }
                val title = text("title") ?: return@sub
                title.text = music.name
                if (title.isCharMissing())
                    title.font = "FZLTHProGBK Heavy"
                while (title.size > 24) {
                    if (title.calcWidth() < 460)
                        break
                    title.size --
                }
                if (title.size > 24)
                    return@sub

                val text2 = title.copy(id="title2")
                // TODO: Split by line width
                val mid = music.name.length / 2
                title.text = music.name.substring(0,  mid)
                text2.text = music.name.substring(mid)
                addAfter(title, text2)
            }
            sub("bottom") {
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

    suspend fun Theme.scoreInfo(
        chart: ChartInfo,
        record: Record?
    ): Container = this["chart"].modify {
        background = "base_${chart.difficulty.name}.png"
        sub("level-container") {
            val nowColor = levelChartColor(chart.difficulty)
            text("icon") {
                color = nowColor
            }
            text("level") {
                color = nowColor
                if (chart.levelValue != 0.0)
                    text = chart.levelValue.toString()
            }
        }
        record ?.let {
            sub("fixed-acc") {
                text("achievement") {
                    text = Rate.toString(record.achievement)
                }
            }
            sub("fixed-rank") {
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
            sub("dxstar-container") {
                text("value") {
                    text = "${record.deluxeScore}/${chart.maxDeluxeScore}"
                }
                image("icon") {
                    val stars = DeluxeScore.stars(record.deluxeScore, record.chart.maxDeluxeScore)
                    src = "icon_dxstar_$stars.png"
                }
            }
        }
        sub("note-designer") {
            val designer = text("designer") ?: return@sub

            designer.text = chart.notesDesigner
            if (designer.isCharMissing())
                designer.font = "FZLTHProGBK Heavy"
            while (designer.size > 13) {
                if (designer.calcWidth() < 180)
                    break
                designer.size --
            }
            if (designer.size > 13)
                return@sub

            val fullText = designer.text
            val mid = fullText.length / 2
            val text2 = designer.copy(id="title2")
            designer.text = fullText.substring(0,  mid)
            text2.text = fullText.substring(mid)
            addAfter(designer, text2)
        }
        if (chart.levelValue == 0.0) {
            sub("fixed-acc") {
                text("achievement") {
                    text = "无此难度"
                }
            }
        }
    }

    /**
     * Template Music Info & Score generation.
     */
    suspend fun templateInfoScore(
        music: MusicInfo,
        records: List<Record>? = null
    ): Bitmap {
        val theme = themes["info"]!!
        val main = theme.main().modify {
            sub("upper") {
                image("cover") {
                    src = "../../covers/${music.resourceId}.png"
                }
                sub("header/wrapper") header@ {
                    headerInfo(this@header, music)
                }
                music.charts.forEach { chart ->
                    val record = records?.firstOrNull { it.chart.difficulty == chart.difficulty }
                    add(theme.scoreInfo(chart, record))
                }
                if (music.genre != MusicGenre.Utage && music.charts.none { it.difficulty == MusicDifficulty.ReMaster }) {
                    add(theme.scoreInfo(music.fakeReMaster, null))
                }
            }
        }
        return theme.render(main)
    }
}