package xyz.xszq.bot.chunithm.controller

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import xyz.xszq.bot.*
import xyz.xszq.bot.chunithm.Chunithm
import xyz.xszq.bot.chunithm.Chunithm.Companion.textMode
import xyz.xszq.bot.chunithm.component.image.FilterParams
import xyz.xszq.bot.chunithm.exception.FilterNoResultException
import xyz.xszq.bot.chunithm.exception.NoDataException
import xyz.xszq.bot.chunithm.exception.NotSupportedException
import xyz.xszq.bot.chunithm.music.ChartInfo
import xyz.xszq.bot.chunithm.music.MusicDifficulty
import xyz.xszq.bot.chunithm.music.RecordsResponse
import xyz.xszq.bot.chunithm.music.UserQueryParams
import xyz.xszq.bot.chunithm.query.ComboQuery
import xyz.xszq.bot.chunithm.query.ComboQuery.filterCharts
import xyz.xszq.bot.chunithm.query.ComboQuery.filterMusics
import xyz.xszq.bot.chunithm.query.ComboQuery.filterRecords
import xyz.xszq.bot.chunithm.query.ComboQuery.isAllRequired
import xyz.xszq.bot.chunithm.query.ComboQuery.isDetailed
import xyz.xszq.bot.chunithm.query.ComboQuery.isPlate
import xyz.xszq.bot.chunithm.query.ComboQuery.params
import xyz.xszq.bot.chunithm.query.Filter
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.exception.NotFoundException
import java.util.concurrent.ConcurrentHashMap

@Suppress("unused")
class ImageController(
    override val chunithm: Chunithm
): Controller(chunithm) {
    private val scoreListCaches = ConcurrentHashMap<String, ScoreListCache>()

    override suspend fun setRoute() = rhythm {
        // b50 及扩展功能
        listOf(30, 50).forEach { total ->
            commandEndsWith(total.toString()) { (command, queryArgs) ->
                var user: UserQueryParams? = null
                runCatching {
                    when (command) {
                        "b" -> {
                            user = chunithm.query.getQueryParams(this, queryArgs ?: "")
                            handleRating(user)
                        }
                        "r" -> {
                            user = chunithm.query.getQueryParams(this, queryArgs ?: "")
                            handleRecent(total, user)
                        }
                        "歌" -> {
                            user = chunithm.query.getQueryParams(this)
                            val musicQuery = queryArgs ?: ""
                            handleMusicRating(total, user, musicQuery)
                        }
                        else -> {
                            user = chunithm.query.getQueryParams(this, queryArgs ?: "")
                            handleCombo(total, user, command)
                        }
                    }
                }.onFailure { e ->
                    handleError(this, e, user)
                }
            }
        }
        // 成绩列表
        commandEndsWith(listOf("分数列表", "分数表", "成绩列表", "成绩表")) { (command, pageArg) ->
            val page = pageArg ?.toIntOrNull() ?: 1
            var user: UserQueryParams? = null
            runCatching {
                user = chunithm.query.getQueryParams(this)
                handleScoreList(command, user, page)
            }.onFailure { e ->
                handleError(this, e, user)
            }
        }
        // 定数表
        commandEndsWith("定数表") { (command, _) ->
            runCatching {
                handleLevelList(command)
            }.onFailure { e->
                handleError(this, e, null)
            }
        }
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
            send(event, text)
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
                        at("💯我也要查", "/chu " + command.trim())
//                    link("随心配", "https://otmdb.cn/bot/maimai/combo", enter = true, id = "2")
//                    at("🎨修改设置", "设置mai", enter = true, id = "3")
                    }
                    page?.let {
                        if (totalPages == null || totalPages <= 1)
                            return@let
                        row {
                            if (page > 1)
                                at("⬅️上一页", "/chu $command ${page - 1}", enter = true, id = "4")
                            if (page < totalPages)
                                at("➡️下一页", "/chu $command ${page + 1}", enter = true, id = "5")
                        }
                    }
                }
            }
        }
    }

    suspend fun MessageEvent.handleRating(
        user: UserQueryParams
    ) {
        val (response, api) = chunithm.query.rating(user)
        if (response.oldRatingList.isEmpty() && response.newRatingList.isEmpty()) {
            throw NoDataException(api = api)
        }
        val (elapsed, result) = countTime {
            chunithm.image.rating.bests(response, api.name)
        }
        result.sendResultImage("b50", this, "生成时间：${elapsed}ms")
    }
    suspend fun MessageEvent.handleRecent(
        total: Int,
        user: UserQueryParams
    ) {
        val (response, api) = chunithm.query.recent(user)
        val (elapsed, result) = countTime {
            chunithm.image.rating.comboBests(
                player = response.player,
                settings = response.settings,
                allRecords = response.records,
                filterParams = FilterParams(
                    newestVersion = chunithm.chunithmData.newestVersion,
                    isAllRequired = true,
                    isDetailed = true
                ),
                api = api.name
            )
        }
        result.sendResultImage("r${total}", this, "生成时间：${elapsed}ms")
    }
    suspend fun MessageEvent.handleCombo(
        total: Int,
        user: UserQueryParams,
        combo: String,
    ) {
        val filters = ComboQuery.filters(combo) ?: return
        val musics = filters.filterMusics(chunithm.musics())
        if (musics.isEmpty()) {
            throw FilterNoResultException()
        }
        val filterParams = filters.params(combo)

        val (response, api) = chunithm.query.records(user, musics)
        val filtered = filters.filterRecords(response.records) ?: emptyList()
        val (elapsed, result) = countTime {
            chunithm.image.rating.comboBests(
                player = response.player,
                settings = response.settings,
                allRecords = filtered,
                filterParams = filterParams,
                api = api.name
            )
        }
        result.sendResultImage("${combo}${total}", this, "生成时间：${elapsed}ms")
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
                    row { at("⬇试一试", "/chu 歌50 ") }
                }
            }
            return
        }
        val (music, difficulty) = selectMusic(
            "歌$total",
            musicQuery,
            true
        ) ?: return

        val (info, api) = chunithm.query.rating(user)
        val recordResponse = chunithm.query.record(user, music)

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
            chunithm.image.rating.comboBests(
                player = info.player,
                settings = info.settings,
                allRecords = List(total) { record },
                filterParams = FilterParams(
                    newestVersion = chunithm.chunithmData.newestVersion,
                    isAllRequired = true,
                    isDetailed = true
                ),
                api = api.name
            )
        }
        result.sendResultImage("歌50", this, "生成时间：${elapsed}ms")
    }
    suspend fun MessageEvent.handleScoreList(
        combo: String,
        user: UserQueryParams,
        page: Int
    ) {
        cleanScoreListCache()

        val filters = ComboQuery.filters(combo) ?: throw FilterNoResultException()
        val musics = filters.filterMusics(chunithm.musics())
        val filterParams = filters.params(combo)

        val key = user.cacheKey(combo)
        val response = scoreListCaches[key] ?.takeIf {
            it.command == combo && !it.isExpired()
        } ?.response ?: chunithm.query.records(user, musics).first.also { response ->
            scoreListCaches[key] = ScoreListCache(
                command = combo,
                response = response,
                expiresAt = System.currentTimeMillis() + SCORE_LIST_CACHE_TTL
            )
        }
        val filtered = filters.filterRecords(response.records) ?: emptyList()
        if (filterParams.isAllRequired && filtered.size > 1500) {
            throw NotSupportedException("您查询的记录过多，全分数列表最多支持1500条记录")
        }
        val (elapsed, result) = countTime {
            chunithm.image.rating.scoreList(
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
            "生成时间：${elapsed}ms",
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

        chunithm.image.level.level(
            charts = charts,
            records = null,
            title = "${combo}定数表",
            filterParams = filterParams
        ).sendResultImage("${combo}定数表", this)
    }


    fun filterCharts(
        filters: List<Filter>,
        preFiltered: List<ChartInfo>? = null
    ): Pair<List<ChartInfo>, Boolean> {
        if (preFiltered != null)
            return Pair(preFiltered, filters.isDetailed())
        val raw = filters.filterCharts(chunithm.musics())
        var detailed = filters.isDetailed()

        if (filters.any { it.type.matchesChart } || filters.isAllRequired())
            return Pair(raw, detailed)
        var charts = if (filters.isPlate()) {
            raw.filter { it.difficulty >= MusicDifficulty.Master }
        } else {
            val result = mutableListOf<ChartInfo>()
            raw.groupBy { it.music.id }.values.forEach { levelCharts ->
                val maxD = levelCharts.maxOf { it.difficulty }
                if (maxD >= MusicDifficulty.Ultima)
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
        this.encodeToData(EncodedImageFormat.JPEG, 95).use { data ->
            file.writeBytes(data!!.bytes)
        }
        this.close()
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
        this.encodeToData(EncodedImageFormat.JPEG, 95).use { data ->
            val bytes = data!!.bytes
            val uploaded = event.bot.cos.uploadBinary(bytes, suffix = ".jpg")
            handle.invoke(event, uploaded.url)
            chunithm.scope.launch {
                delay(10000L)
                event.bot.cos.deleteFromCos(uploaded.filename)
            }
        }
        this.close()
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
            is UserQueryParams.Self -> "self"
            is UserQueryParams.Username -> "username:${username.lowercase()}"
            is UserQueryParams.FriendCode -> "friend:${friendCode.lowercase()}"
        }
        return "${event.sender.id}:$target:$command"
    }
}