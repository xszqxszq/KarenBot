package xyz.xszq.bot.maimai.controller

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import xyz.xszq.bot.*
import xyz.xszq.bot.Maimai.Companion.textMode
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.exception.NotFoundException
import xyz.xszq.bot.maimai.component.image.FilterParams
import xyz.xszq.bot.maimai.exception.FilterNoResultException
import xyz.xszq.bot.maimai.exception.NoDataException
import xyz.xszq.bot.maimai.exception.NotSupportedException
import xyz.xszq.bot.maimai.music.*
import xyz.xszq.bot.maimai.payload.LocalCourseInfo
import xyz.xszq.bot.maimai.query.ComboQuery
import xyz.xszq.bot.maimai.query.ComboQuery.filterCharts
import xyz.xszq.bot.maimai.query.ComboQuery.filterMusics
import xyz.xszq.bot.maimai.query.ComboQuery.filterRecords
import xyz.xszq.bot.maimai.query.ComboQuery.isAllRequired
import xyz.xszq.bot.maimai.query.ComboQuery.isDetailed
import xyz.xszq.bot.maimai.query.ComboQuery.isPlate
import xyz.xszq.bot.maimai.query.ComboQuery.params
import xyz.xszq.bot.maimai.query.ComboQuery.requiresType
import xyz.xszq.bot.maimai.query.Filter
import xyz.xszq.bot.maimai.toSimple
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

@Suppress("unused")
class ImageController(
    override val maimai: Maimai
): Controller(maimai) {
    private var tips = mutableListOf<String>()
    private val scoreListCaches = ConcurrentHashMap<String, ScoreListCache>()

    override suspend fun setRoute() = rhythm {
        channel<Pair<String, CompletableDeferred<String>>>("parse-image") { (urlsJson, deferred) ->
            val urls = json.decodeFromString<List<String>>(urlsJson)
            val client = maimai.pluginLoader.llmClient
            val results = if (client != null)
                maimai.query.parseImage(client, urls)
            else
                emptyList()
            deferred.complete(json.encodeToString(results))
        }
        tips = maimai.config.tips.toMutableList()
        // b50 / b40 及扩展功能
        listOf(50, 40).forEach { total ->
            commandEndsWith(total.toString()) { raw ->
                val args = raw.split(" ")
                val command = args.first()

                if (command == "随心配") {
                    reply("https://otmdb.cn/bot/maimai/combo")
                    return@commandEndsWith
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
        startsWith(listOf("info", "minfo")) { text ->
            queryByTextOrImage(text) { musicQuery ->
                var user: UserQueryParams? = null
                runCatching {
                    user = maimai.query.getQueryParams(this)
                    handleInfoScore(user, musicQuery)
                }.onFailure { e ->
                    handleError(this, e, user)
                }
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
            reply {
                line(bold("查询结果"))
                line()
                line(image(url, "img #${width}px #${height}px"))
                line()
                line(text ?: "")
                keyboard {
                    row {
                        at("💯我也要查", "/mai " + command.trim())
                        link("随心配", "https://otmdb.cn/bot/maimai/combo")
                        at("🎨修改设置", "设置mai", enter = true)
                    }
                    page?.let {
                        if (totalPages == null || totalPages <= 1)
                            return@let
                        row {
                            if (page > 1)
                                at("⬅️上一页", "/mai $command ${page - 1}", enter = true)
                            if (page < totalPages)
                                at("➡️下一页", "/mai $command ${page + 1}", enter = true)
                        }
                    }
                }
            }
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
//        useTempFile(suffix = ".jpg") { file ->
//            val radar = maimai.image.radar.generate((response.newRatingList + response.oldRatingList).map {
//                it.chart
//            }) ?: return@useTempFile
//            radar.encodeToData(EncodedImageFormat.JPEG, 90) ?.bytes ?.let {
//                file.writeBytes(it)
//                reply(xyz.xszq.bot.message.Image(file))
//            }
//        }
        result.sendResultImage("b$total", this, "生成时间：${elapsed}ms\n${randomTips()?:""}")
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
        result.sendResultImage("r$total", this, "生成时间：${elapsed}ms\n${randomTips()?:""}")
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
        result.sendResultImage("${combo}$total", this, "生成时间：${elapsed}ms\n${randomTips()?:""}")
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
            reply(help.newLine()) {
                brief("舞萌DX", help)
                keyboard {
                    row { at("⬇试一试", "/mai 歌50 ") }
                }
            }
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
        result.sendResultImage("歌$total", this, "生成时间：${elapsed}ms\n${randomTips()?:""}")
    }
    suspend fun MessageEvent.handleScoreList(
        combo: String,
        user: UserQueryParams,
        page: Int
    ) {
        cleanScoreListCache()

        val filters = ComboQuery.filters(combo) ?: throw FilterNoResultException()
        val musics = filters.filterMusics(maimai.musics())
        val filterParams = filters.params(combo)

        val key = user.cacheKey(combo)
        val response = scoreListCaches[key] ?.takeIf {
            it.command == combo && !it.isExpired()
        } ?.response ?: maimai.query.records(user, musics).first.also { response ->
            scoreListCaches[key] = ScoreListCache(
                command = combo,
                response = response,
                expiresAt = System.currentTimeMillis() + SCORE_LIST_CACHE_TTL
            )
        }
        val filtered = filters.filterRecords(response.records) ?: emptyList()
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
            "生成时间：${elapsed}ms\n${randomTips()?:""}",
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

        val allCharts = filters.filterCharts(
            maimai.musics().filter { it.genre != MusicGenre.Utage }
        )

        val (response, _) = maimai.query.records(
            user,
            filters.filterMusics(maimai.musics().filter { it.genre != MusicGenre.Utage })
        )

        maimai.image.level.level(
            charts = charts,
            records = response.records,
            title = "${combo}完成表",
            filterParams = filterParams,
            showProgress = true,
            progressData = computeProgressData(filters, allCharts, response.records),
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

        val allCharts = filters.filterCharts(
            maimai.musics().filter { it.genre != MusicGenre.Utage }
        )
        val (response, _) = maimai.query.records(
            user,
            filters.filterMusics(maimai.musics().filter { it.genre != MusicGenre.Utage })
        )

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
            filterParams = filterParams,
            showProgress = true,
            progressData = computeProgressData(filters, allCharts, response.records),
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

    private fun computeProgressData(
        filters: List<Filter>,
        allCharts: List<ChartInfo>,
        records: List<Record>?,
    ): Map<MusicDifficulty, Pair<Int, Int>> {
        val total = allCharts.groupBy { it.difficulty }.mapValues { it.value.size }
        val allChartSet = allCharts.toSet()
        val completed: Map<MusicDifficulty, Int> = if (records != null) {
            val filtered = filters.filterRecords(records, true)
            if (filtered != null) {
                filtered.filter { it.chart in allChartSet }
                    .groupBy { it.chart.difficulty }
                    .mapValues { it.value.size }
            } else {
                val requiresType = filters.requiresType()
                records.filter { it.chart in allChartSet }
                    .groupBy { it.chart.difficulty }
                    .mapValues { entry ->
                        entry.value.count { record ->
                            when (requiresType) {
                                RequiresType.Achievement -> record.achievement >= 800000
                                RequiresType.Combo -> record.comboStatus != ComboStatus.None
                                RequiresType.Sync -> record.syncStatus != SyncStatus.None
                            }
                        }
                    }
            }
        } else {
            emptyMap()
        }
        return MusicDifficulty.entries.filter { it != MusicDifficulty.Utage }.associateWith { diff ->
            (total[diff] ?: 0) to (completed[diff] ?: 0)
        }
    }

    fun filterCharts(
        filters: List<Filter>,
        preFiltered: List<ChartInfo>? = null
    ): Pair<List<ChartInfo>, Boolean> {
        if (preFiltered != null)
            return Pair(preFiltered, filters.isDetailed())
        val raw = filters.filterCharts(maimai.musics())
        var detailed = filters.isDetailed()

        if (filters.any { it.type.matchesChart } || filters.isAllRequired())
            return Pair(raw, detailed)
        var charts = if (filters.isPlate()) {
            raw.filter { it.difficulty >= MusicDifficulty.Master }
        } else {
            val result = mutableListOf<ChartInfo>()
            raw.groupBy { it.music.id }.values.forEach { levelCharts ->
                val maxD = levelCharts.maxOf { it.difficulty }
                if (maxD >= MusicDifficulty.ReMaster)
                    result.addAll(levelCharts.filter { it.difficulty >= MusicDifficulty.Master })
                else
                    result.addAll(levelCharts.filter { it.difficulty == maxD })
            }
            result
        }
        if (charts.size > 400) {
            detailed = true

            val masterOnly = charts.filter { it.difficulty >= MusicDifficulty.Master }
            charts = when {
                masterOnly.size <= 200 -> masterOnly
                else -> masterOnly.filter { it.levelValue >= 14.0 }
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
        private const val SCORE_LIST_CACHE_TTL = 5 * 60 * 1000L

        private data class ScoreListCache(
            val command: String,
            val response: RecordsResponse,
            val expiresAt: Long
        ) {
            fun isExpired(now: Long = System.currentTimeMillis()) = now > expiresAt
        }

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

    private fun cleanScoreListCache() {
        scoreListCaches.entries.removeIf { it.value.isExpired() }
    }

    private fun UserQueryParams.cacheKey(command: String): String {
        val target = when (this) {
            is UserQueryParams.QQ -> "qq:$qq"
            is UserQueryParams.Username -> "username:${username.lowercase()}"
        }
        return "${event.sender.id}:$target:$command"
    }
}