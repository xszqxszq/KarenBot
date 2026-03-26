package xyz.xszq.bot.component

import korlibs.image.bitmap.Bitmap
import korlibs.image.font.CharacterSet
import korlibs.image.font.toBitmapFont
import korlibs.io.file.std.localCurrentDirVfs
import korlibs.io.util.isDigit
import korlibs.io.util.toStringDecimal
import korlibs.math.toIntFloor
import kotlinx.coroutines.flow.filter
import xyz.xszq.bot.Filter
import xyz.xszq.bot.Maimai
import xyz.xszq.bot.Query
import xyz.xszq.bot.api.MaimaiAPI
import xyz.xszq.bot.music.*
import xyz.xszq.bot.pagination
import xyz.xszq.bot.payload.LocalCourseInfo
import xyz.xszq.shinobu.*
import kotlin.math.min

/**
 * Image Layer for Maimai Plugin.
 */
class MaimaiImage(
    val maimai: Maimai
) {
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
        optimize()
    }

    suspend fun optimize() {
        val rating = themes["rating"]!!
        rating.templates.first { it.id == "main" }.modify {
            sub("upper/best-35") {
                parallel = true
            }
            sub("upper/best-15") {
                parallel = true
            }
            sub("footer") {
                text("copyright") {
                    val chars = text.toSet().map { it.code }.sorted().toIntArray()
                    setFont(rating, CharacterSet(chars))
                }
            }
        }
        rating.templates.first { it.id == "music" }.modify {
            fun Text.setFont(chars: CharacterSet = CharacterSet.NUMBERS) = setFont(rating, chars)
            text("index-id") {
                setFont()
            }
            text("title") {
                val chars = maimai.musics().map { it.name.toList() }.flatten().toSet()
                    .map { it.code }.sorted().toIntArray()
                setFont(CharacterSet(chars))
            }
            text("achievement-integer") {
                setFont()
            }
            text("achievement-decimal") {
                setFont(CharacterSet(('0'..'9').joinToString("") + "."))
            }
            text("achievement-percent") {
                setFont(CharacterSet("%"))
            }
            text("level-rating") {
                setFont(CharacterSet(('0'..'9').joinToString("") + ".→"))
            }
        }

        val level = themes["level"]!!
        level.templates.first { it.id == "main" }.modify {
            parallel = true
            sub("footer") {
                text("copyright") {
                    val chars = text.toSet().map { it.code }.sorted().toIntArray()
                    setFont(rating, CharacterSet(chars))
                }
            }
        }
        level.templates.first { it.id == "group" }.modify {
            text("level") {
                setFont(level, CharacterSet(('0'..'9').joinToString("") + ".+"))
            }
            sub("musics") {
                parallel = true
            }
        }
        level.templates.first { it.id == "music" }.modify {
            sub("cover/info/id-container") {
                text("id") {
                    setFont(level, CharacterSet(('0'..'9').joinToString("")))
                }
            }
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
            background = "covers/${record.music.resourceId}.bmp"
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
        nowVersion: GameVersion,
        backend: MaimaiAPI,
        isAllRequired: Boolean,
        isFitLevelValues: Boolean = false,
        lambda: suspend List<Record>.() -> List<Record>?
    ): Bitmap? {
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
        nowVersion: GameVersion,
        backend: MaimaiAPI,
        isAllRequired: Boolean,
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
        val noB15 = isAllRequired || if (nowVersion == maimai.local.newestVersion)
            records.filter { it.music.version == nowVersion }.size < 15 || records.none { it.music.version != nowVersion }
        else
            false
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
            background = "covers/${chart.music.resourceId}.bmp"
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
        filters: List<Filter>?,
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
        val filtered = Query.filterRecords(filters, records ?: emptyList())
        val completed = charts.associateWith { chart ->
            filtered ?.firstOrNull {
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
                if (music.genre == MusicGenre.Utage)
                    music.charts.first().let { chart ->
                        add(theme.scoreInfo(chart, records?.firstOrNull()))
                    }
                else
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

    /**
     * Generate a single Music Info for Course.
     * @param chart The chart to render.
     * @param record The record to display.
     */
    suspend fun Theme.courseMusic(
        chart: ChartInfo,
        achievement: Int,
        life: Int,
        damage: Int
    ) = this["music"].modify {
        image("result") {
            if (life <= 0)
                src = "result_2.png"
        }
        sub("music") {
            background = "base_${chart.difficulty.name}.png"
            image("cover") {
                src = "../../covers/${chart.music.resourceId}.png"
            }
            sub("info/header") {
                image("type") {
                    src = "type_${chart.music.type.value}.png"
                }
                sub("title") {
                    text("title") {
                        text = chart.music.name
                    }
                }
            }
            sub("info/level") {
                image("level") {
                    src = "level_${chart.difficulty.value}_level.png"
                }
                sub("value") {
                    val digits = chart.level.filter { it.isDigit() }
                    digits.forEachIndexed { index, value ->
                        add(Image(
                            src="level_${chart.difficulty.value}_$value.png",
                            margin = Spacing(0, 0, 0,
                                if (index != digits.length - 1) -12 else 0
                            )
                        ))
                    }
                }
                image("plus") {
                    src = "level_${if (chart.level.endsWith("+")) chart.difficulty.value else 5}_plus.png"
                }
            }
            sub("info/achievement") {
                val part1 = (achievement / 10000).toString().padStart(3, ' ')
                val part2 = (achievement % 10000).toString().padStart(4, '0')
                val type = when {
                    achievement < 800000 -> 1
                    achievement < 970000 -> 2
                    else -> 3
                }
                part1.forEach { char ->
                    if (char == ' ')
                        add(Image(
                            src="score_1_none.png",
                            margin=Spacing(0, 0, 0, -10)
                        ))
                    else
                        add(Image(
                            src="score_1_${type}_${char}.png",
                            margin=Spacing(0, 0, 0, -10)
                        ))
                }
                add(Image(
                    src="score_3_${type}_dot.png",
                    margin=Spacing(0, 0, -6, -15)
                ))
                part2.forEach { char ->
                    add(Image(
                        src="score_3_${type}_${char}.png",
                        margin=Spacing(0, 0, 0, -10)
                    ))
                }
                add(Image(src="percent_2_${type}.png"))
            }
            sub("info/damage") {
                text("damage") {
                    text = "-${damage}"
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
    ): Bitmap {
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
        val theme = themes["course"]!!
        val main = theme.main().modify {
            val realId = course.id % 10000
            background = when {
                realId <= 1010 -> "background_1.png"
                realId <= 1112 -> "background_2.png"
                else -> "background_3.png"
            }
            sub("upper/musics") {
                scores.forEachIndexed { index, (chart, record) ->
                    add(theme.courseMusic(
                        chart, record ?.achievement ?: 0, remains[index], damages[index]
                    ))
                }
            }
            sub("upper/info/status") {
                if (life <= 0)
                    background = "final_2.png"
                image("course") {
                    src = "title_${course.id}.png"
                }
            }
            sub("upper/info/life") {
                val type = when {
                    life >= 100 -> 1
                    life >= 10 -> 2
                    life >= 1 -> 3
                    else -> 4
                }
                background = "life_$type.png"
                life.toString().forEach { value ->
                    add(Image(src = "life_${type}_${value}.png"))
                }
            }
            sub("upper/info/detail/header/life") {
                text("value") {
                    text = course.life.toString()
                }
            }
            sub("upper/info/detail/achievement") {
                val total = scores.sumOf { it.second ?.achievement ?: 0 }
                val type = when {
                    total < 800000 * scores.size -> 1
                    total < 970000 * scores.size -> 2
                    else -> 3
                }

                val part1 = (total / 10000).toString().padStart(3, ' ')
                val part2 = (total % 10000).toString().padStart(4, '0')
                part1.forEach { char ->
                    if (char == ' ')
                        add(Image(
                            src="score_2_none.png",
                            margin=Spacing(0, 0, 0, -10)
                        ))
                    else
                        add(Image(
                            src="score_2_${type}_${char}.png",
                            margin=Spacing(0, 0, 0, -10)
                        ))
                }
                add(Image(
                    src="score_1_${type}_dot.png",
                    margin=Spacing(0, 0, -18, -18)
                ))
                part2.forEach { char ->
                    add(Image(
                        src="score_1_${type}_${char}.png",
                        margin=Spacing(0, 0, 0, -10)
                    ))
                }
                add(Image(src="percent_1_${type}.png"))
            }
            sub("upper/info/detail/damage") {
                sub("great") {
                    text("value") {
                        text = "-${course.damage.great}"
                    }
                }
                sub("good") {
                    text("value") {
                        text = "-${course.damage.good}"
                    }
                }
                sub("miss") {
                    text("value") {
                        text = "-${course.damage.miss}"
                    }
                }
            }
            sub("upper/info/detail/heal/heal") {
                text("value") {
                    text = "+${course.recover}"
                }
            }
        }
        return theme.render(main)
    }
    companion object {
        fun Text.setFont(theme: Theme, chars: CharacterSet = CharacterSet.NUMBERS) {
            bitmapFont = theme.fontCache[font]!!.toBitmapFont(
                size,
                chars = chars,
                paint = color.hexToRGBA()
            )
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
}