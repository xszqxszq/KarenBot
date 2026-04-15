package xyz.xszq.bot.chunithm.controller

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import xyz.xszq.bot.*
import xyz.xszq.bot.Chunithm.Companion.textMode
import xyz.xszq.bot.chunithm.component.MarkdownTemplates
import xyz.xszq.bot.chunithm.component.MarkdownTemplates.Keyboards.single
import xyz.xszq.bot.chunithm.component.MarkdownTemplates.Templates.selectMusic
import xyz.xszq.bot.chunithm.component.image.FilterParams
import xyz.xszq.bot.chunithm.exception.FilterNoResultException
import xyz.xszq.bot.chunithm.exception.NoDataException
import xyz.xszq.bot.chunithm.exception.NotSupportedException
import xyz.xszq.bot.chunithm.music.MusicDifficulty
import xyz.xszq.bot.chunithm.music.MusicInfo
import xyz.xszq.bot.chunithm.music.UserQueryParams
import xyz.xszq.bot.chunithm.query.ComboQuery
import xyz.xszq.bot.chunithm.query.ComboQuery.filterMusics
import xyz.xszq.bot.chunithm.query.ComboQuery.filterRecords
import xyz.xszq.bot.chunithm.query.ComboQuery.params
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.exception.NotFoundException
import xyz.xszq.bot.message.Markdown
import xyz.xszq.bot.payload.markdown.Keyboard
import xyz.xszq.bot.payload.markdown.MarkdownData

@Suppress("unused")
class ImageController(
    override val chunithm: Chunithm
): Controller(chunithm) {

    override suspend fun setRoute() = rhythm {
        // b50 及扩展功能
        listOf(30, 50).forEach { total ->
            commandEndsWith(total.toString()) { raw ->
                val args = raw.split(" ")
                val command = args.first()

                val queryArgs = args.getOrNull(1) ?: ""
                var user: UserQueryParams? = null
                runCatching {
                    when (command) {
                        "b" -> {
                            user = chunithm.query.getQueryParams(this, queryArgs)
                            handleRating(user)
                        }
                        "r" -> {
                            user = chunithm.query.getQueryParams(this, queryArgs)
                            handleRecent(total, user)
                        }
                        "歌" -> {
                            user = chunithm.query.getQueryParams(this)
                            val musicQuery = args.subList(1, args.size).joinToString(" ")
                            handleMusicRating(total, user, musicQuery)
                        }
                        else -> {
                            user = chunithm.query.getQueryParams(this, queryArgs)
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
                user = chunithm.query.getQueryParams(this)
                handleScoreList(command, user, page)
            }.onFailure { e ->
                handleError(this, e, user)
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
                    at("💯我也要查", "/chu " + command.trim(), id = "1")
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
            }))
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
        result.sendResultImage("r50", this, "生成时间：${elapsed}ms")
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
        result.sendResultImage("${combo}50", this, "生成时间：${elapsed}ms")
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
        val filters = ComboQuery.filters(combo) ?: throw FilterNoResultException()
        val musics = filters.filterMusics(chunithm.musics())
        val filterParams = filters.params(combo)

        val (response, _) = chunithm.query.records(user, musics)
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


    suspend fun MessageEvent.selectMusic(
        type: String,
        args: String,
        needDifficulty: Boolean
    ): Pair<MusicInfo, MusicDifficulty?>? {
        var difficulty = if (needDifficulty) MusicDifficulty.from(args.firstOrNull() ?.toString() ?: "") else null
        val name = difficulty ?.let { args.substring(1, args.length) } ?: args
        var result = chunithm.aliases.search(name)
        if (difficulty != null)
            result = result.filter { it.charts.any { chart -> chart.difficulty == difficulty } }
        if (difficulty != null && result.isEmpty()) {
            difficulty = null
            result = chunithm.aliases.search(args)
        }
        when (result.size) {
            0 -> throw NotFoundException("未找到该歌曲")
            1 -> return Pair(result.first(), difficulty)
            else -> {
                if (textMode())
                    return Pair(result.first(), difficulty)
                else
                    reply(
                        selectMusic(
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

    suspend fun Image.send(
        event: MessageEvent,
        message: String ?= null
    ): Unit = useTempFile { file ->
        val bytes = this.encodeToData(EncodedImageFormat.JPEG, 95)!!.bytes
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
        val bytes = this.encodeToData(EncodedImageFormat.JPEG, 95)!!.bytes
        val uploaded = event.bot.cos.uploadBinary(bytes, suffix = ".jpg")
        handle.invoke(event, uploaded.url)
        chunithm.scope.launch {
            delay(10000L)
            event.bot.cos.deleteFromCos(uploaded.filename)
        }
    }

    companion object {
        suspend fun <T> countTime(block: suspend () -> T): Pair<Long, T> {
            val start = System.currentTimeMillis()
            val result = block()
            return Pair(System.currentTimeMillis() - start, result)
        }
    }
}