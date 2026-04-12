package xyz.xszq.bot.controller

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import xyz.xszq.bot.*
import xyz.xszq.bot.Maimai.Companion.textMode
import xyz.xszq.bot.component.MarkdownTemplates
import xyz.xszq.bot.component.MarkdownTemplates.Keyboards.single
import xyz.xszq.bot.component.image.FilterParams
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.exception.*
import xyz.xszq.bot.message.Markdown
import xyz.xszq.bot.music.*
import xyz.xszq.bot.payload.LocalCourseInfo
import xyz.xszq.bot.payload.markdown.Keyboard
import xyz.xszq.bot.payload.markdown.MarkdownData
import xyz.xszq.bot.query.ComboQuery
import xyz.xszq.bot.query.ComboQuery.filterCharts
import xyz.xszq.bot.query.ComboQuery.filterMusics
import xyz.xszq.bot.query.ComboQuery.filterRecords
import xyz.xszq.bot.query.ComboQuery.isDetailed
import xyz.xszq.bot.query.ComboQuery.isPlate
import xyz.xszq.bot.query.ComboQuery.params
import xyz.xszq.bot.query.Filter
import kotlin.random.Random

@Suppress("unused")
class ImageController(
    override val maimai: Maimai
): Controller(maimai) {
    private var tips = mutableListOf<String>()

    override suspend fun setRoute() = maimai.route("/mai") {
        tips = maimai.config.tips.toMutableList()
        // b50 / b40 及扩展功能
        listOf(50, 40).forEach { total ->
            commandEndsWith(total.toString()) { raw ->
                val args = raw.split(" ")
                val command = args.first()

                when {
                    "id" in command || "查歌" in command || "预览" in command -> return@commandEndsWith
                    command == "设置b" -> return@commandEndsWith
                    command == "随心配" -> {
                        reply("https://otmdb.cn/bot/maimai/combo")
                        return@commandEndsWith
                    }
                }

                val queryArgs = args.getOrNull(1) ?: ""
                var user: UserQueryParams? = null
                runCatching {
                    when (command) {
                        "b" -> {
                            user = maimai.query.getQueryParams(this, queryArgs)
                            handleRating(total, user)
                        }
                        "r" -> {
                            user = maimai.query.getQueryParams(this, queryArgs)
                            handleRecent(total, user)
                        }
                        "歌" -> {
                            user = maimai.query.getQueryParams(this)
                            val musicQuery = args.subList(1, args.size).joinToString(" ")
                            handleMusicRating(total, user, musicQuery)
                        }
                        else -> {
                            user = maimai.query.getQueryParams(this, queryArgs)
                            handleCombo(total, user, command)
                        }
                    }
                }.onFailure { e ->
                    handleError(this, e, user)
                }
            }
        }
        // 成绩列表
        commandEndsWith(listOf("分数列表", "分数表", "成绩列表", "成绩表")) { raw ->
            val args = raw.split(" ")
            val command = args.first()
            val page = args.getOrNull(1) ?.toIntOrNull() ?: 1
            var user: UserQueryParams? = null
            runCatching {
                user = maimai.query.getQueryParams(this)
                handleScoreList(command, user, page)
            }.onFailure { e ->
                handleError(this, e, user)
            }
        }
        // 定数表
        commandEndsWith("定数表") { raw ->
            val args = raw.split(" ")
            val command = args.first()
            runCatching {
                handleLevelList(command)
            }.onFailure { e->
                handleError(this, e, null)
            }
        }
        // 完成表
        commandEndsWith(listOf("完成表", "进度表")) { raw ->
            val args = raw.split(" ")
            val command = args.first()
            if (command.endsWith("未"))
                return@commandEndsWith

            val queryArgs = args.getOrNull(1) ?: ""
            var user: UserQueryParams? = null
            runCatching {
                user = maimai.query.getQueryParams(this, queryArgs)
                handleLevelComplete(command, user)
            }.onFailure { e ->
                handleError(this, e, user)
            }
        }
        // 未完成表
        commandEndsWith(listOf("未完成表", "未完成列表")) { raw ->
            val args = raw.split(" ")
            val command = args.first()
            val queryArgs = args.getOrNull(1) ?: ""
            var user: UserQueryParams? = null
            runCatching {
                user = maimai.query.getQueryParams(this, queryArgs)
                handleLevelIncomplete(command, user)
            }.onFailure { e ->
                handleError(this, e, user)
            }
        }
        // 歌曲信息+成绩
        startsWith(listOf("info", "minfo")) { musicQuery ->
            var user: UserQueryParams? = null
            runCatching {
                user = maimai.query.getQueryParams(this)
                handleInfoScore(user, musicQuery)
            }.onFailure { e ->
                handleError(this, e, user)
            }
        }
        // 段位表
        val courseSubscribes = buildMap {
            maimai.maimaiData.courses.values.forEach { course ->
                put(course.name.toSimple().lowercase(), course)
            }
            courseAliases.forEach { (id, aliases) ->
                maimai.maimaiData.courses[id]?.let { course ->
                    aliases.forEach { alias ->
                        put(alias.toSimple().lowercase(), course)
                    }
                }
            }
        }
        courseSubscribes.forEach { (name, course) ->
            startsWith(name) { args ->
                var user: UserQueryParams? = null
                runCatching {
                    user = maimai.query.getQueryParams(this)
                    handleCourse(course, user)
                }.onFailure { e ->
                    handleError(this, e, user)
                }
            }
        }
        startsWith("段位表") { raw ->
            val args = raw.split(" ", limit = 2).map { it.trim() }
            val name = args.firstOrNull() ?.lowercase() ?: run {
                reply("使用方法：段位表 段位名称\n\t例：段位表 十段\n\t例：段位表 随机紫超上")
                return@startsWith
            }
            val course = maimai.maimaiData.courses.values.firstOrNull {
                it.name.toSimple().lowercase() == name
            } ?: run {
                courseAliases.firstNotNullOfOrNull { (id, aliases) ->
                    if (aliases.any { it.toSimple().lowercase() == name })
                        id
                    else
                        null
                } ?.let { id ->
                    maimai.maimaiData.courses[id]
                }
            }
            course ?.let {
                var user: UserQueryParams? = null
                runCatching {
                    user = maimai.query.getQueryParams(this, args.getOrNull(1))
                    handleCourse(course, user)
                }.onFailure { e ->
                    handleError(this, e, user)
                }
            } ?: run {
                reply("未找到该段位。\n使用方法：段位表 段位名称\n\t例：段位表 十段\n\t例：段位表 随机紫超上")
            }
        }
    }
    fun randomTips(): String? {
        if (Random(System.currentTimeMillis() * 2).nextDouble() < 0.5)
            return "TIPS: " + tips.random(Random(System.currentTimeMillis() * 5))
        return null
    }

    suspend fun MessageEvent.selectMusic(
        type: String,
        args: String,
        needDifficulty: Boolean
    ): Pair<MusicInfo, MusicDifficulty?>? {
        var difficulty = if (needDifficulty) MusicDifficulty.from(args.firstOrNull() ?.toString() ?: "") else null
        val name = difficulty ?.let { args.substring(1, args.length) } ?: args
        var result = maimai.aliases.search(name)
        if (difficulty != null)
            result = result.filter { it.charts.any { chart -> chart.difficulty == difficulty } }
        if (difficulty != null && result.isEmpty()) {
            difficulty = null
            result = maimai.aliases.search(args)
        }
        when (result.size) {
            0 -> throw NotFoundException("未找到该歌曲")
            1 -> return Pair(result.first(), difficulty)
            else -> {
                if (textMode())
                    return Pair(result.first(), difficulty)
                else
                    reply(
                        MarkdownTemplates.Templates.selectMusic(
                        title = "您要查找的歌曲可能是：",
                        type = type,
                        keyword = args,
                        difficulty = difficulty,
                        result = result
                    ))
            }
        }
        return null
    }

    suspend fun Image?.sendResultImage(
        command: String,
        event: MessageEvent,
        text: String ?= null,
        page: Int ?= null,
        totalPages: Int ?= null
    ) = event.run {
        this@sendResultImage ?: return@run
        if (textMode()) {
            send(this, text)
            return
        }
        upload(event) { url ->
            reply(Markdown(MarkdownData(buildString {
                appendLine("**查询结果**")
                appendLine()
                appendLine("![img #${width}px #${height}px]($url)")
                appendLine()
                append(text ?: "")
            }), Keyboard.create {
                row {
                    at("💯我也要查", command, id = "1")
                    link("随心配", "https://otmdb.cn/bot/maimai/combo", enter = true, id = "2")
                    at("🎨修改设置", "设置mai", enter = true, id = "3")
                }
                page?.let {
                    if (totalPages == null || totalPages <= 1) return@let
                    row {
                        if (page > 1)
                            at("⬅️上一页", "$command ${page - 1}", enter = true, id = "4")
                        if (page < totalPages)
                            at("➡️下一页", "$command ${page + 1}", enter = true, id = "5")
                    }
                }
            }))
        }
    }

    suspend fun MessageEvent.handleRating(
        total: Int,
        user: UserQueryParams
    ) {
        val (response, api) = maimai.query.rating(user)
        if (response.oldRatingList.isEmpty() && response.newRatingList.isEmpty()) {
            throw NoDataException(api = api)
        }
        val (elapsed, result) = countTime {
            maimai.image.rating.bests(total, response, api.name)
        }
        result.sendResultImage("b50", this, "生成时间：${elapsed}ms\r${randomTips()?:""}")
    }
    suspend fun MessageEvent.handleRecent(
        total: Int,
        user: UserQueryParams
    ) {
        val (response, api) = maimai.query.recent(user)
        val (elapsed, result) = countTime {
            maimai.image.rating.comboBests(
                total = total,
                player = response.player,
                settings = response.settings,
                allRecords = response.records,
                filterParams = FilterParams(
                    newestVersion = maimai.maimaiData.newestVersion,
                    isFitLevelValue = false,
                    isAllRequired = true,
                    isDetailed = true
                ),
                api = api.name
            )
        }
        result.sendResultImage("r50", this, "生成时间：${elapsed}ms\r${randomTips()?:""}")
    }
    suspend fun MessageEvent.handleCombo(
        total: Int,
        user: UserQueryParams,
        combo: String,
    ) {
        val filters = ComboQuery.filters(combo) ?: return
        val musics = filters.filterMusics(maimai.musics())
        if (musics.isEmpty()) {
            throw FilterNoResultException()
        }
        val filterParams = filters.params(combo)

        val (response, api) = maimai.query.records(user, musics)
        val filtered = filters.filterRecords(response.records) ?: emptyList()
        val (elapsed, result) = countTime {
            maimai.image.rating.comboBests(
                total = total,
                player = response.player,
                settings = response.settings,
                allRecords = filtered,
                filterParams = filterParams,
                api = api.name
            )
        }
        result.sendResultImage("${combo}50", this, "生成时间：${elapsed}ms\r${randomTips()?:""}")
    }

    suspend fun MessageEvent.handleMusicRating(
        total: Int,
        user: UserQueryParams,
        musicQuery: String
    ) {
        if (musicQuery.isBlank()) {
            val help = buildString {
                appendLine("该功能可查看填充${total}遍同一首歌的b${total}。")
                appendLine("使用方法：歌${total} (难度)id/名称/别称")
                appendLine("\t例：歌${total} 紫茄子")
                appendLine("\t例：歌${total} kib")
            }.trim()
            if (textMode())
                reply(help.newLine())
            else
                reply(MarkdownTemplates.Templates.brief("舞萌DX", help)
                    .toMessage(single("歌50 ", "⬇试一试")))
            return
        }
        val (music, difficulty) = selectMusic(
            "歌$total",
            musicQuery,
            true
        ) ?: return

        val (info, api) = maimai.query.rating(user)
        val recordResponse = maimai.query.record(user, music)

        val record = difficulty ?.let {
            recordResponse.firstOrNull {
                it.chart.difficulty == difficulty
            } ?: throw NotFoundException("未查询到该歌曲该难度的游玩记录")
        } ?: run {
            recordResponse.sortedBy {
                -it.chart.difficulty.value
            }.firstOrNull {
                it.achievement != 0
            }
        } ?: throw NotFoundException("未查询到该歌曲的游玩记录")

        val (elapsed, result) = countTime {
            maimai.image.rating.comboBests(
                total = total,
                player = info.player,
                settings = info.settings,
                allRecords = List(total) { record },
                filterParams = FilterParams(
                    newestVersion = maimai.maimaiData.newestVersion,
                    isFitLevelValue = false,
                    isAllRequired = true,
                    isDetailed = true
                ),
                api = api.name
            )
        }
        result.sendResultImage("歌50", this, "生成时间：${elapsed}ms\r${randomTips()?:""}")
    }
    suspend fun MessageEvent.handleScoreList(
        combo: String,
        user: UserQueryParams,
        page: Int
    ) {
        val filters = ComboQuery.filters(combo) ?: throw FilterNoResultException()
        val musics = filters.filterMusics(maimai.musics())
        val filterParams = filters.params(combo)

        val (response, _) = maimai.query.records(user, musics)
        val filtered = filters.filterRecords(response.records) ?: emptyList()
        if (filterParams.isAllRequired && filtered.size > 1500) {
            throw NotSupportedException("您查询的记录过多，全分数列表最多支持1500条记录")
        }
        val (elapsed, result) = countTime {
            maimai.image.rating.scoreList(
                player = response.player,
                settings = response.settings,
                allRecords = filtered,
                filterParams = filterParams,
                page = page
            )
        }
        val (image, nowPage, totalPages) = result

        image.sendResultImage(
            "${combo}分数列表",
            this,
            "生成时间：${elapsed}ms\r${randomTips()?:""}",
            page = nowPage,
            totalPages = totalPages
        )
    }
    suspend fun MessageEvent.handleLevelList(
        combo: String
    ) {
        val filters = ComboQuery.filters(combo) ?: throw FilterNoResultException()
        val (charts, isDetailed) = filterCharts(filters)

        val filterParams = filters.params(combo)
        filterParams.isDetailed = isDetailed

        maimai.image.level.level(
            charts = charts,
            records = null,
            title = "${combo}定数表",
            filterParams = filterParams
        ).sendResultImage("${combo}定数表", this, randomTips())
    }
    suspend fun MessageEvent.handleLevelComplete(
        combo: String,
        user: UserQueryParams
    ) {
        val filters = ComboQuery.filters(combo) ?: throw FilterNoResultException()

        val (charts, isDetailed) = filterCharts(filters)
        if (charts.isEmpty())
            throw FilterNoResultException()

        val filterParams = filters.params(combo)
        filterParams.isDetailed = isDetailed

        val musics = charts.map { it.music }.toSet().toList()
        val (response, _) = maimai.query.records(user, musics)

        maimai.image.level.level(
            charts = charts,
            records = response.records,
            title = "${combo}完成表",
            filterParams = filterParams
        ).sendResultImage("${combo}完成表", this, randomTips())
    }
    suspend fun MessageEvent.handleLevelIncomplete(
        combo: String,
        user: UserQueryParams
    ) {
        val filters = ComboQuery.filters(combo) ?: throw FilterNoResultException()

        val (charts, _) = filterCharts(filters)
        if (charts.isEmpty())
            throw FilterNoResultException()

        val musics = charts.map { it.music }.toSet().toList()
        val (response, _) = maimai.query.records(user, musics)

        val completed = filters.filterRecords(response.records, true) ?: emptyList()
        val remains = charts.filter { chart ->
            completed.none { it.chart == chart }
        }
        val (filtered, isDetailed) = filterCharts(filters, preFiltered = remains)

        val filterParams = filters.params(combo)
        filterParams.isDetailed = isDetailed

        if (filtered.isEmpty()) {
            reply("恭喜您已完成所有谱面！")
            return
        }

        maimai.image.level.level(
            charts = filtered,
            records = response.records,
            title = "${combo}未完成表",
            filterParams = filterParams
        ).sendResultImage("${combo}未完成表", this, randomTips())
    }
    suspend fun MessageEvent.handleInfoScore(
        user: UserQueryParams,
        musicQuery: String
    ) {
        val (music, _) = selectMusic("info", musicQuery, false)
            ?: return
        val records = maimai.query.record(user, music)
        maimai.image.score.template(
            music,
            records
        ).sendResultImage("info id${music.id}", this, randomTips())
    }

    suspend fun MessageEvent.handleCourse(
        course: LocalCourseInfo,
        user: UserQueryParams
    ) {
        val charts = if (course.random) {
            val difficulties =
                if (course.name.startsWith("MASTER"))
                    listOf(MusicDifficulty.Master, MusicDifficulty.ReMaster)
                else
                    listOf(MusicDifficulty.Expert)
            val list = maimai.musics().flatMap { it.charts }.filter {
                it.difficulty in difficulties && it.levelValue in course.lower..course.upper
            }
            List(4) { list.shuffled().first() }
        } else {
            course.musics.mapNotNull { music ->
                maimai.music(music.id) ?.charts?.firstOrNull {
                    it.difficulty.value == music.difficulty
                }
            }
        }
        val musics = charts.map { it.music }.toSet().toList()
        val (response, _) = maimai.query.records(user, musics)

        val scores = charts.map { chart ->
            Pair(chart, response.records.firstOrNull { record ->
                chart == record.chart
            })
        }
        maimai.image.course.template(course, scores).sendResultImage(
            course.name.toSimple().lowercase(),
            this,
            randomTips()
        )
    }

    fun filterCharts(
        filters: List<Filter>,
        preFiltered: List<ChartInfo>? = null
    ): Pair<List<ChartInfo>, Boolean> {
        var charts = preFiltered ?: run {
            filters.filterCharts(
                maimai.musics().filter { it.genre != MusicGenre.Utage }
            )
        }

        var detailed = filters.isDetailed()
        if (filters.isPlate()) {
            charts = if (charts.distinctBy { it.music.id }.size > 250) {
                detailed = true
                charts.filter { it.difficulty >= MusicDifficulty.Master && it.levelValue >= 14 }
            } else {
                charts.filter { it.difficulty == MusicDifficulty.Master }
            }
        } else {
            val grouped = charts.groupBy { it.music.id }.values
            charts = mutableListOf()
            grouped.forEach { levelCharts ->
                val maxDifficulty = levelCharts.maxOf { it.difficulty }
                when {
                    maxDifficulty >= MusicDifficulty.ReMaster -> {
                        charts.addAll(levelCharts.filter { it.difficulty >= MusicDifficulty.Master })
                    }
                    else -> {
                        charts.addAll(levelCharts.filter { it.difficulty >= MusicDifficulty.Expert })
                    }
                }
            }
            if (grouped.size > 480) {
                detailed = true
                charts = when {
                    charts.count { it.difficulty >= MusicDifficulty.Master && it.levelValue >= 14 } > 10 -> {
                        charts.filter { it.difficulty >= MusicDifficulty.Master && it.levelValue >= 14 }
                    }
                    charts.count { it.difficulty >= MusicDifficulty.Master && it.levelValue >= 13.6 } > 10 -> {
                        charts.filter { it.difficulty >= MusicDifficulty.Master && it.levelValue >= 13.6 }
                    }
                    charts.count { it.difficulty >= MusicDifficulty.Expert && it.levelValue >= 13 } > 10 -> {
                        charts.filter { it.difficulty >= MusicDifficulty.Expert && it.levelValue >= 13 }
                    }
                    else -> throw FilterTooManyException()
                }
            }
        }
        return Pair(charts, detailed)
    }

    suspend fun Image.send(
        event: MessageEvent,
        message: String ?= null
    ): Unit = useTempFile { file ->
        val bytes = this.encodeToData(EncodedImageFormat.JPEG, 90)!!.bytes
        file.writeBytes(bytes)
        message ?.let {
            event.reply(xyz.xszq.bot.message.Image(file) + it.toPlainText())
        } ?: run {
            event.reply(xyz.xszq.bot.message.Image(file))
        }
    }
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun Image.upload(
        event: MessageEvent,
        handle: suspend MessageEvent.(String) -> Unit
    ): Unit = useTempFile(suffix = ".jpg") { file ->
        val bytes = this.encodeToData(EncodedImageFormat.JPEG, 90)!!.bytes
        val uploaded = event.bot.cos.uploadBinary(bytes, suffix = ".jpg")
        handle.invoke(event, uploaded.url)
        maimai.scope.launch {
            delay(10000L)
            event.bot.cos.deleteFromCos(uploaded.filename)
        }
    }

    companion object {
        val courseAliases = buildMap {
            put(452201, listOf("随机红初级", "随机expert初级", "红初级"))
            put(452202, listOf("随机红中级", "随机expert中级", "红中级"))
            put(452203, listOf("随机红上级", "随机expert上级", "红上级"))
            put(452204, listOf("随机红超上级", "随机红超上", "随机expert超上级", "红超上级", "红超上"))
            put(452301, listOf("随机紫初级", "随机初级", "随机master初级", "紫初级", "初级"))
            put(452302, listOf("随机紫中级", "随机中级", "随机master中级", "紫中级", "中级"))
            put(452303, listOf("随机紫上级", "随机上级", "随机master上级", "紫上级", "上级"))
            put(452304, listOf("随机紫超上级", "随机紫超上", "随机超上级", "随机超上", "随机master超上级", "紫超上级", "紫超上", "超上"))
        }

        suspend fun <T> countTime(block: suspend () -> T): Pair<Long, T> {
            val start = System.currentTimeMillis()
            val result = block()
            return Pair(System.currentTimeMillis() - start, result)
        }
    }
}