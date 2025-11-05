package xyz.xszq.bot.controller

import korlibs.image.bitmap.Bitmap
import korlibs.image.format.ImageEncodingProps
import korlibs.image.format.JPEG
import korlibs.math.toIntFloor
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.xszq.bot.*
import xyz.xszq.bot.Maimai.Companion.textMode
import xyz.xszq.bot.database.MaimaiSettingsTable
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.exception.NotFoundException
import xyz.xszq.bot.music.ChartInfo
import xyz.xszq.bot.music.MusicDifficulty
import xyz.xszq.bot.music.MusicInfo
import xyz.xszq.bot.music.Rating
import xyz.xszq.bot.music.Record
import xyz.xszq.bot.payload.LocalCourseInfo
import xyz.xszq.shinobu.countTime
import kotlin.random.Random

@Suppress("unused")
class ImageController(
    override val maimai: Maimai
): Controller(maimai) {
    override fun setRoute() = maimai.route("/mai") {
        commandEndsWith("50") { raw ->
            if (raw.toIntOrNull() != null)
                return@commandEndsWith
            val args = raw.split(" ")
            val command = args.first()
            if ("id" in command || "查歌" in command)
                return@commandEndsWith
            val arg = args.getOrNull(1) ?: ""
            when (command) {
                "设置b" -> return@commandEndsWith
                "b" -> handleRating50(this, arg)
                "歌" -> handleMusicRating(this, args.subList(1, args.size).joinToString(" "), 35)
                "随心配" -> reply("https://otmdb.cn/bot/maimai/combo")
                else -> runCatching {
                    handle50(this, command, arg)
                }.onFailure { e ->
                    e.printStackTrace()
                }
            }
            if (command == "b") {
                return@commandEndsWith
            }
        }
        commandEndsWith("40") { raw ->
            val args = raw.split(" ")
            val command = args.first()
            val arg = args.getOrNull(1) ?: ""
            when (command) {
                "b" -> handleRating40(this, arg)
                "歌" -> handleMusicRating(this, args.subList(1, args.size).joinToString(" "), 25)
                else -> runCatching {
                    handle40(this, command, arg)
                }.onFailure { e ->
                    e.printStackTrace()
                }
            }
            if (command == "b") {
                return@commandEndsWith
            }
        }
        commandEndsWith(listOf("分数列表", "分数表", "成绩列表", "成绩表")) { raw ->
            val args = raw.split(" ")
            val command = args.first()
            val page = args.getOrNull(1) ?.toIntOrNull() ?: 1
            kotlin.runCatching {
                handleScoreList(this, command, page)
            }.onFailure { e ->
                reply("生成失败")
                e.printStackTrace()
            }
        }
        commandEndsWith("定数表") { raw ->
            val args = raw.split(" ")
            val command = args.first()
            kotlin.runCatching {
                handleLevelList(this, command)
            }.onFailure {
                if (it.message == "TooMany") {
                    reply("您当前查询的曲目过多")
                } else {
                    reply("生成失败")
                    it.printStackTrace()
                }
            }
        }
        commandEndsWith("完成表") { raw ->
            val args = raw.split(" ")
            val command = args.first()
            val arg = args.getOrNull(1) ?: ""
            kotlin.runCatching {
                handleLevelCompletes(this, command, arg)
            }.onFailure { e ->
                when {
                    e.message == "TooMany" -> {
                        reply("您当前查询的曲目过多")
                    }
                    else -> {
                        reply("生成失败")
                        e.printStackTrace()
                    }
                }
            }
        }
        startsWith(listOf("info", "minfo")) { args ->
            val found = runCatching {
                selectMusic("info", args, false)
            }.onFailure { e ->
                when (e) {
                    is NotFoundException -> reply(buildString {
                        appendLine("未找到相关歌曲，请检查拼写。")
                        appendLine("使用方法：info id/歌名/别名")
                    })
                    else -> e.printStackTrace()
                }
                return@startsWith
            }.getOrNull() ?: return@startsWith
            val (music, _) = found
            handleInfoScore(this, music)
        }

        maimai.local.courses.values.forEach { course ->
            startsWith(course.name.toSimple()) { args ->
                handleCourse(this, course, args)
            }
        }
    }

    val tips = listOf(
        "b50查询支持叠加多种条件查询啦，如「东方寸50」「术力口13ap50」「maistar50」",
        "分数列表也支持多种条件叠加，如「东方锁血分数列表」「jack13+大将分数列表」",
        "查询完成表/进度支持自由组合条件啦，如「v家将完成表」「东方13将完成表」「翠楼屋极进度」",
        "如果您使用旧版QQ无法查看新版UI，可以发送「兼容模式」",
        "b50的头像和姓名框样式支持更换，点击「修改设置」来试试吧",
        "新版查歌搜索结果可以点击哦",
        "可以试一试「舞萌开字母」猜歌游戏"
    )
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
        val textMode = MaimaiSettingsTable[sender.id, "text-mode"] == "1"

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
            0 -> throw NotFoundException()
            1 -> return Pair(result.first(), difficulty)
            else -> {
                if (textMode)
                    return Pair(result.first(), difficulty)
                else
                    reply(MarkdownTemplates.Templates.resultSimple(
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
    suspend fun Bitmap?.sendResultImage(
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
            reply(MarkdownTemplates.Templates.image(
                url, Pair(width, height), command, text, page, totalPages
            ))
        }
    }

    suspend fun handleRating50(
        event: MessageEvent,
        args: String
    ) {
        var time = 0L
        maimai.query.rating(event, args) { response, backend ->
            if (response.ratingList.isEmpty() && response.newRatingList.isEmpty()) {
                noData()
                return@rating null
            }
            countTime {
                maimai.image.templateRating(response, backend = backend)
            }.let { (elapsed, result) ->
                time = elapsed
                result
            }
        }.sendResultImage("b50", event, "生成时间：${time}ms\r${randomTips()?:""}")
    }
    suspend fun handleRating40(
        event: MessageEvent,
        args: String
    ) {
        var time = 0L
        maimai.query.rating(event, args) { response, backend ->
            response.ratingList.forEach {
                it.rating = Rating.calcOld(it.chart, it.achievement)
            }
            response.newRatingList.forEach {
                it.rating = Rating.calcOld(it.chart, it.achievement)
            }
            response.rating = response.ratingList.take(25).sumOf { it.rating } +
                    response.newRatingList.take(15).sumOf { it.rating } +
                    Rating.courseOld(response.course)
            countTime {
                maimai.image.templateRating(response, old = 25, new = 15, backend = backend)
            }.let { (elapsed, result) ->
                time = elapsed
                result
            }
        }.sendResultImage("b40", event, "生成时间：${time}ms\r${randomTips()?:""}")
    }
    suspend fun handle50(
        event: MessageEvent,
        fullCommand: String,
        args: String
    ) {
        val filters = Query.filters(fullCommand) ?: return
        val musics = Query.filterMusics(filters, maimai.musics())
        val nowVersion = Query.filterNowVersion(filters) ?: maimai.local.newestVersion
        if (musics.isEmpty()) {
            event.reply(maimai.query.noRecords)
            return
        }
        val noB15 = Query.isAllRequired(filters) || if (nowVersion == maimai.local.newestVersion)
            musics.filter { it.version == nowVersion }.size < 15 || musics.none { it.version != nowVersion }
        else
            false
        val isFitLevelValues = Query.isFitLevelValues(filters)
        var time = 0L
        maimai.query.records(event, musics, args) { response, backend ->
            countTime {
                maimai.image.templateBest50(response, noB15, nowVersion, backend, isFitLevelValues) {
                    Query.filterRecords(filters, this)
                }
            }.let { (elapsed, result) ->
                time = elapsed
                result
            }
        }.sendResultImage(fullCommand+"50", event, "生成时间：${time}ms\r${randomTips()?:""}")
    }
    suspend fun handle40(
        event: MessageEvent,
        fullCommand: String,
        args: String
    ) {
        val filters = Query.filters(fullCommand)
        val musics = Query.filterMusics(filters, maimai.musics())
        val nowVersion = Query.filterNowVersion(filters) ?: maimai.local.newestVersion
        val noB15 = Query.isAllRequired(filters) || if (nowVersion == maimai.local.newestVersion)
            musics.filter { it.version == nowVersion }.size < 15 || musics.none { it.version != nowVersion }
        else
            false
        val isFitLevelValues = Query.isFitLevelValues(filters)
        var time = 0L
        maimai.query.records(event, musics, args) { response, backend ->
            countTime {
                maimai.image.templateBest40(response, noB15, nowVersion, backend, isFitLevelValues) {
                    Query.filterRecords(filters, this)
                }
            }.let { (elapsed, result) ->
                time = elapsed
                result
            }
        }.sendResultImage(fullCommand+"40", event, "生成时间：${time}ms\r${randomTips()?:""}")
    }

    suspend fun handleMusicRating(
        event: MessageEvent,
        args: String,
        old: Int = 35
    ) = event.run {
        val bests = old + 15
        if (args.isBlank()) {
            val help = buildString {
                appendLine("该功能可查看填充${bests}遍同一首歌的b${bests}。")
                appendLine("使用方法：歌${bests} (难度)id/名称/别称")
                appendLine("\t例：歌${bests} 紫茄子")
                appendLine("\t例：歌${bests} kib")
            }.trim()
            if (textMode())
                reply(help.newLine())
            else
                reply(MarkdownTemplates.Templates.brief(
                    "舞萌DX",
                    help
                ).toMessage(
                    MarkdownTemplates.Keyboards.tryIt("歌50")
                ))
            return@run
        }
        val found = runCatching {
            selectMusic("歌$bests", args, true)
        }.onFailure { e ->
            when (e) {
                is NotFoundException -> reply("未找到该歌曲！")
                else -> e.printStackTrace()
            }
            return@run
        }.getOrNull() ?: return@run
        val (music, difficulty) = found
        runCatching {
            handleMusicRating(event, args, old, music, difficulty)
        }.onFailure { e ->
            when (e) {
                is NotFoundException -> reply("未查询到该歌曲${if (difficulty != null) "该难度" else ""}的游玩记录。")
                else -> e.printStackTrace()
            }
        }
    }
    suspend fun handleMusicRating(
        event: MessageEvent,
        args: String,
        old: Int = 35,
        music: MusicInfo,
        difficulty: MusicDifficulty ?= null
    ) = maimai.query.rating(event, "") { rating, backend ->
        maimai.query.record(event, music) { response ->
            val record = difficulty ?.let {
                response.firstOrNull { it.chart.difficulty == difficulty } ?: throw NotFoundException()
            } ?: run {
                response.sortedBy { -it.chart.difficulty.value }.firstOrNull { it.achievement != 0 }
            } ?: throw NotFoundException()
            if (old == 25)
                record.rating = Rating.calcOld(record.chart, record.achievement)
            rating.ratingList = List(old) { record }
            rating.newRatingList = List(15) { record }
            rating.rating = rating.ratingList.sumOf { it.rating } + rating.newRatingList.sumOf { it.rating }
            maimai.image.templateRating(rating, old = old, backend = backend)
        }
    }.sendResultImage("歌50 ${difficulty?.brief?:""}${music.id}", event, randomTips())
    suspend fun handleScoreList(
        event: MessageEvent,
        fullCommand: String,
        page: Int
    ) {
        val filters = Query.filters(fullCommand)
        val musics = Query.filterMusics(filters, maimai.musics())
        val all = Query.isAllRequired(filters)
        var time = 0L
        var numPage = 1
        var totalPages = 1
        val isFitLevelValues = Query.isFitLevelValues(filters)
        maimai.query.records(event, musics) { response, _ ->
            countTime {
                val (image, nowPage, nowPages) = maimai.image.templateScoreList(
                    response, fullCommand, page, all, isFitLevelValues
                ) {
                    Query.filterRecords(filters, this) ?.also {
                        if (all && it.size > 1000) {
                            reply("您查询的记录过多，全分数列表最多支持1000条记录")
                            return@templateScoreList null
                        }
                    }
                } ?: return@countTime null
                numPage = nowPage
                totalPages = nowPages
                image
            }.let { (elapsed, result) ->
                time = elapsed
                result
            }
        }.sendResultImage(
            "${fullCommand}分数列表", event, "生成时间：${time}ms\r${randomTips()?:""}",
            page = numPage,
            totalPages = totalPages
        )
    }
    suspend fun handleLevelList(
        event: MessageEvent,
        fullCommand: String
    ) = event.run {
        val filters = Query.filters(fullCommand)
        val (charts, detailed) = filterCharts(filters)
        val isFitLevelValues = Query.isFitLevelValues(filters)
        maimai.image.templateLevel(
            charts,
            fullCommand + "定数表",
            detailed,
            isFitLevelValues = isFitLevelValues
        ).sendResultImage(fullCommand + "定数表", event, randomTips())
    }
    suspend fun handleLevelCompletes(
        event: MessageEvent,
        fullCommand: String,
        args: String
    ) = event.run {
        val filters = Query.filters(fullCommand)
        if (Query.noRecordFilter(filters)) {
            if (filters == null || filters.isEmpty()) {
                reply(maimai.query.noRecords)
                return@run
            }
        }
        val (charts, detailed) = filterCharts(filters)
        if (charts.isEmpty()) {
            reply(maimai.query.noRecords)
            return@run
        }
        val isFitLevelValues = Query.isFitLevelValues(filters)
        val musics = charts.map { it.music }.toSet().toList()
        maimai.query.records(event, musics, args) { response, _ ->
            maimai.image.templateLevel(
                charts,
                fullCommand + "完成表",
                detailed,
                Query.filterTypes(filters),
                response.records,
                isFitLevelValues = isFitLevelValues
            )
        }.sendResultImage(fullCommand + "完成表", event, randomTips())
    }
    suspend fun handleInfoScore(
        event: MessageEvent,
        music: MusicInfo
    ) = event.run {
        maimai.query.record(event, music) { response ->
            maimai.image.templateInfoScore(
                music,
                response)
        }.sendResultImage("info id${music.id}", event, randomTips())
    }

    suspend fun handleCourse(
        event: MessageEvent,
        course: LocalCourseInfo,
        args: String
    ) = event.run {
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
        maimai.query.records(event, musics, args) { response, _ ->
            val scores = charts.map { chart ->
                Pair(chart, response.records.firstOrNull { record ->
                    chart == record.chart
                })
            }
            maimai.image.templateCourse(course, scores)
                .sendResultImage(course.name.toSimple(), event, randomTips())
        }
    }

    fun filterCharts(
        filters: List<Filter>?
    ): Pair<List<ChartInfo>, Boolean> {
        val filters = filters?.toMutableList()
        var charts = Query.filterCharts(filters, maimai.musics())
        var detailed = Query.isDetailed(filters)
        if (Query.isPlate(filters)) {
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
                        charts.add(levelCharts.maxBy { it.difficulty })
                    }
                }
            }
            if (grouped.size > 480) {
                detailed = true
                if (charts.count { it.difficulty >= MusicDifficulty.Master && it.levelValue >= 14 } > 10)
                    charts = charts.filter { it.difficulty >= MusicDifficulty.Master && it.levelValue >= 14 }
                else
                    throw Exception("TooMany")
            }
        }
        return Pair(charts, detailed)
    }

    suspend fun Bitmap.send(
        event: MessageEvent,
        message: String ?= null
    ): Unit = useTempFile { file ->
        val bytes = JPEG.encode(this, ImageEncodingProps(quality = 0.85))
        file.writeBytes(bytes)
        message ?.let {
            event.reply(xyz.xszq.bot.message.Image(file) + it.toPlainText())
        } ?: run {
            event.reply(xyz.xszq.bot.message.Image(file))
        }
    }
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun Bitmap.upload(
        event: MessageEvent,
        handle: suspend MessageEvent.(String) -> Unit
    ): Unit = useTempFile(suffix = ".jpg") { file ->
        val bytes = JPEG.encode(this, ImageEncodingProps(quality = 0.85))
        file.writeBytes(bytes)
        val uploaded = event.bot.cos.upload(file)
        handle.invoke(event, uploaded.url)
        GlobalScope.launch {
            delay(10000L)
            event.bot.cos.deleteFromCos(uploaded.filename)
        }
    }

    suspend fun MessageEvent.noData() {
        if (textMode())
            reply(buildString {
                appendLine("您似乎尚未导入舞萌DX分数，请查看数据导入教程：")
                appendLine("水鱼查分器：https://otmdb.cn/jump/maimaidxprober_import")
                appendLine("落雪查分器：https://otmdb.cn/jump/lxnsprober_import")
            }.trim().newLine())
        else reply(MarkdownTemplates.Templates.IMPORT_DATA)
    }
}