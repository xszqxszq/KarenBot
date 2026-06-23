package xyz.xszq.bot.maimai.controller

import korlibs.io.file.VfsFile
import korlibs.io.file.std.localCurrentDirVfs
import xyz.xszq.bot.*
import xyz.xszq.bot.Maimai.Companion.textMode
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.event.ReplyAble
import xyz.xszq.bot.exception.IllegalArgsException
import xyz.xszq.bot.exception.IllegalOperationException
import xyz.xszq.bot.exception.NeedHelpException
import xyz.xszq.bot.exception.NotFoundException
import xyz.xszq.bot.ffmpeg.FFMpegFileType
import xyz.xszq.bot.ffmpeg.FFMpegTask
import xyz.xszq.bot.maimai.component.AliasAudit
import xyz.xszq.bot.maimai.component.MarkdownTemplates
import xyz.xszq.bot.maimai.component.MarkdownTemplates.Templates.selectMusic
import xyz.xszq.bot.maimai.database.MaimaiMusicAliasesTable
import xyz.xszq.bot.maimai.database.MaimaiMusicAliasesVoteTable
import xyz.xszq.bot.maimai.music.ChartInfo
import xyz.xszq.bot.maimai.music.MusicDifficulty
import xyz.xszq.bot.maimai.music.MusicInfo
import xyz.xszq.bot.maimai.query.ComboQuery
import xyz.xszq.bot.maimai.query.ComboQuery.filterCharts
import xyz.xszq.bot.maimai.query.ComboQuery.filterMusics
import xyz.xszq.bot.maimai.query.ComboQuery.isSingleChartSelected
import xyz.xszq.bot.message.Audio
import xyz.xszq.bot.subscribe.CommandNotMatchedException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.random.Random

@Suppress("unused")
class MusicController(
    override val maimai: Maimai
): Controller(maimai) {
    private lateinit var aliasAudit: AliasAudit
    private val previewDir = "./data/maimai/preview/"
    private val notFound = "未查找到相关的歌曲，请检查拼写是否有误。"

    private val maxResults = 10
    private val maxResultsLong = 40

    private val jacketUrl = maimai.config.tokens["assets-jacket"] ?: throw Exception("assets-jacket missing")

    override suspend fun setRoute() = rhythm {
        aliasAudit = AliasAudit(maimai)
        // 根据 ID 精准查找
        startsWith("id") { raw ->
            val id = raw.toIntOrNull() ?: throw CommandNotMatchedException()
            val music = maimai.music(id) ?: throw CommandNotMatchedException()
            reply(music.infoText(), music.infoMD(jacketUrl))
        }
        button("maimai-id") {
            val id = data.toIntOrNull() ?: return@button
            val music = maimai.music(id) ?: return@button
            if (textMode())
                reply(music.infoText())
            else
                reply(music.infoMD(jacketUrl))
        }
        MusicDifficulty.entries.forEach { difficulty ->
            val name = difficulty.names.last()
            startsWith(listOf("${name}id", name)) { raw ->
                val id = raw.toIntOrNull() ?: throw CommandNotMatchedException()
                val music = maimai.music(id) ?: throw CommandNotMatchedException()
                val chart = music.charts.firstOrNull { it.difficulty == difficulty } ?: return@startsWith
                reply(chart.infoText(), chart.infoMD(jacketUrl))
//
//                val radar = maimai.image.radar.generate(chart, 500, false) ?: return@startsWith
//                useTempFile { file ->
//                    val bytes = radar.encodeToData(EncodedImageFormat.JPEG, 90)!!.bytes
//                    file.writeBytes(bytes)
//                    reply(Image(file))
//                }
            }
        }

        // 随机歌曲
        startsWith("随个") { query ->
            val filters = ComboQuery.filters(query)
            val musics = filters.filterMusics(maimai.musics())
            if (musics.isEmpty()) {
                reply(notFound)
                return@startsWith
            }
            val selected = musics.random(Random(System.currentTimeMillis()))
            reply(selected.infoText(), selected.infoMD(jacketUrl))
        }

        // 模糊搜索
        startsWith("查歌") { name ->
            queryByTextOrImage(name, "使用方法：查歌 歌曲名称/别名") { music ->
                search(music)
            }
        }
        endsWith(listOf("是什么歌", "是什么歌？")) { name ->
            queryByTextOrImage(name, "使用方法：xx是什么歌") { music ->
                search(music)
            }
        }
        button("maimai-search-word") {
            val args = data.split("\n", limit = 2)
            val name = args[0]
            val page = args[1].toInt()
            search(name, page)
        }

        // 条件查歌
        startsWith("定数查歌") { raw ->
            val (levels, page) = raw.levelArgs()
            when {
                levels.size == 1 -> {
                    searchLevel(levels[0], levels[0], page)
                }
                levels.size >= 2 -> {
                    searchLevel(levels[0], levels[1], page)
                }
                else -> reply(buildString {
                    appendLine("使用方法：定数查歌 [定数] [定数] [页数]")
                    appendLine("例：定数查歌 13.0")
                    appendLine("例：定数查歌 13.4 13.8")
                    appendLine("例：定数查歌 12.2 12.5 2")
                })
            }
        }
        button("maimai-search-level") {
            val args = data.split("\n", limit = 2)
            val (begin, end) = args[0].split(":").map { it.toDouble() }
            val page = args[1].toInt()
            searchLevel(begin, end, page)
        }
        startsWith("拟合定数查歌") { raw ->
            val (levels, page) = raw.levelArgs()
            when {
                levels.size == 1 -> {
                    searchLevelFit(levels[0], levels[0], page)
                }
                levels.size >= 2 -> {
                    searchLevelFit(levels[0], levels[1], page)
                }
                else -> reply(buildString {
                    appendLine("使用方法：拟合定数查歌 [定数] [定数] [页数]")
                    appendLine("例：拟合定数查歌 13.0")
                    appendLine("例：拟合定数查歌 13.4 13.8")
                    appendLine("例：拟合定数查歌 12.2 12.5 2")
                })
            }
        }
        button("maimai-search-level-fit") {
            val args = data.split("\n", limit = 2)
            val (begin, end) = args[0].split(":").map { it.toDouble() }
            val page = args[1].toInt()
            searchLevelFit(begin, end, page)
        }
        startsWith("谱师查歌") { name ->
            when {
                name.isNotBlank() -> {
                    searchDesigner(name, 1)
                }
                else -> {
                    reply(buildString {
                        appendLine("使用方法：谱师查歌 [名称]")
                        appendLine("例：谱师查歌 翠楼屋")
                        appendLine("例：谱师查歌 mai-Star")
                    })
                }
            }
        }
        button("maimai-search-designer") {
            val args = data.split("\n", limit = 2)
            val name = args[0]
            val page = args[1].toInt()
            searchDesigner(name, page)
        }
        startsWith("版本查歌") { name ->
            when {
                name.isNotBlank() -> {
                    searchVersion(name, 1)
                }
                else -> {
                    reply(buildString {
                        appendLine("使用方法：版本查歌 [版本名]")
                        appendLine("例：版本查歌 舞萌DX 2025")
                        appendLine("例：版本查歌 ORANGE")
                    })
                }
            }
        }
        button("maimai-search-version") {
            val args = data.split("\n", limit = 2)
            val version = args[0]
            val page = args[1].toInt()
            searchVersion(version, page)
        }
        startsWith("曲师查歌") { name ->
            when {
                name.isNotBlank() -> {
                    searchArtist(name, 1)
                }
                else -> {
                    reply(buildString {
                        appendLine("使用方法：曲师查歌 [曲师名]")
                        appendLine("例：曲师查歌 t+pazolite")
                        appendLine("例：曲师查歌 豚乙女")
                    })
                }
            }
        }
        button("maimai-search-artist") {
            val args = data.split("\n", limit = 2)
            val artist = args[0]
            val page = args[1].toInt()
            searchArtist(artist, page)
        }
        startsWith("正则查歌") { raw ->
            if (raw.isBlank()) {
                reply(buildString{
                    appendLine("使用方法：正则查歌 正则表达式")
                    appendLine("例：正则查歌 ^(?i)w.*(?i)ing")
                })
                return@startsWith
            }
            val regex = kotlin.runCatching {
                Regex(raw)
            }.getOrNull() ?: run {
                reply("请使用正确的正则表达式查询。")
                return@startsWith
            }
            searchRegex(raw, regex)
        }
        startsWith(listOf("BPM查歌", "bpm查歌")) { raw ->
            if (raw.isBlank()) {
                reply(buildString {
                    appendLine("使用方法：BPM查歌 [BPM] [页数]")
                    appendLine("例：BPM查歌 180")
                    appendLine("例：BPM查歌 180 2")
                })
                return@startsWith
            }
            val args = raw.split(" ")
            val bpm = args.firstOrNull()?.toIntOrNull() ?: run {
                reply("请输入正确的BPM值。")
                return@startsWith
            }
            val page = args.getOrNull(1)?.toIntOrNull() ?: 1
            searchBPM(bpm, page)
        }
        button("maimai-search-bpm") {
            val args = data.split("\n", limit = 2)
            val bpm = args[0].toInt()
            val page = args[1].toInt()
            searchBPM(bpm, page)
        }
        // 条件搜索
        startsWith("搜索") { query ->
            if (!searchCombo(query)) {
                reply("未找到相关歌曲。")
            }
        }
        endsWith(listOf("有什么歌", "有什么歌？", "有哪些歌", "有哪些歌？")) { query ->
            if (!searchCombo(query)) {
                reply("未找到相关歌曲。")
            }
        }
        button("maimai-search-combo") {
            val args = data.split("\n", limit = 2)
            val query = args[0]
            val page = args[1].toInt()
            if (!searchCombo(query, page)) {
                reply("未找到相关歌曲。")
            }
        }
        // 别名查询/设置
        endsWith(listOf("有什么别名", "有什么别名？")) { name ->
            queryByTextOrImage(name) {
                val music = maimai.aliases.search(name).firstOrNull() ?: return@queryByTextOrImage
                val aliases = MaimaiMusicAliasesTable[music]
                    .filter { it.first != music.name }
                    .take(maxResultsLong)
                    .joinToString("\n") { (alias, _) ->
                        alias
                    }
                val text = buildString {
                    appendLine("${music.id}.${music.name} 有如下别名：")

                    appendLine(aliases)
                    appendLine()
                    appendLine("可以@机器人使用“添加别名 id 别名”来添加别名。")
                }.trim()
                reply(text.newLine()) {
                    brief("别名列表", text)
                    keyboard {
                        row {
                            at("添加别名", "/mai 添加别名 id${music.id}")
                        }
                    }
                }
            }
        }
        startsWith("添加别名") { raw ->
            runCatching {
                addAlias(raw)
            }.onFailure { e ->
                val help = buildString {
                    appendLine("使用方法：添加别名 id/名称 别名")
                    appendLine("\t例：添加别名 834 潘")
                    appendLine("\t例：添加别名 茄子 qzk")
                }.trim().newLine()
                when (e) {
                    is NeedHelpException -> reply(help)
                    is NotFoundException -> reply("未找到该歌曲。$help")
                    is IllegalArgsException -> reply(e.message ?: "")
                    is IllegalOperationException -> reply(e.message ?: "")
                    else -> e.printStackTrace()
                }
            }
        }
        startsWith("删除别名") { raw ->
            if (!isAdmin()) return@startsWith
            val args = raw.trim().split(" ", limit = 2).filter { it.isNotBlank() }
            if (args.size < 2) {
                reply("使用方法：删除别名 id/名称 别名")
                return@startsWith
            }
            val (name, alias) = args.take(2)
            val music = maimai.aliases.search(name).firstOrNull() ?: run {
                reply("未找到该歌曲。")
                return@startsWith
            }
            val existing = MaimaiMusicAliasesTable[music, alias]
            if (existing == null) {
                reply("该别名不存在。")
                return@startsWith
            }
            MaimaiMusicAliasesTable.remove(music, alias)
            maimai.aliases.delete(music.id, alias)
            reply("别名删除成功。")
        }
        startsWith("今日舞萌") {
            val time = LocalDate.now()
            val random = Random((sender.id + time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .hashCode())
            val hash = random.nextInt(1, 101)
            var h = hash
            val dailyMusic = maimai.musics().random(random)
            var message = dailyMusic.infoText()
            message = buildString {
                appendLine(time.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 E")))
                appendLine("今日幸运指数为 $hash")
                dailyOps.shuffled(random).take(6).forEach {
                    val now = h and 3
                    if (now == 3)
                        appendLine("宜 $it")
                    else if (now == 0)
                        appendLine("忌 $it")
                    h = h shr 2
                }
                appendLine("今日推荐歌曲：")
            }.toPlainText() + message
            reply(message)
        }
        startsWith("预览") { raw ->
            runCatching {
                queryByTextOrImage(raw) { musicQuery ->
                    val (music, _) = selectMusic("预览", musicQuery, false) ?: return@queryByTextOrImage
                    val file = localCurrentDirVfs[previewDir]["${music.resourceId}.ogg"]
                    if (!file.exists()) {
                        return@queryByTextOrImage
                    }
                    file.toPCM { pcm ->
                        reply(Audio(pcm))
                    }
                }
            }
        }
    }

    val dailyOps = listOf(
        "推分", "下埋", "越级", "拼机", "单刷", "练底力", "练手法", "抓准度", "抓绝赞", "收歌", "堵门", "夜勤"
    )

    private suspend fun ReplyAble.showMusics(
        type: String,
        keyword: String,
        result: List<MusicInfo>,
        displayName: String ?= null,
        nowPage: Int = 1,
        totalPages: Int = 1,
    ) {
        when {
            result.isEmpty() -> {
                reply(notFound)
            }
            result.size == 1 && totalPages == 1 -> {
                reply(result.first().infoText(), result.first().infoMD(jacketUrl))
            }
            else -> {
                val hint = if (totalPages != 1)
                    "您要查找的歌曲可能是 ($nowPage / $totalPages)："
                else
                    "您要查找的歌曲可能是："
                reply(buildString {
                    appendLine(hint)
                    result.forEach { music ->
                        appendLine("${music.id}. ${music.name}")
                    }
                }.trim(), selectMusic(
                    title = hint,
                    type = type,
                    keyword = keyword,
                    difficulty = null,
                    result = result,
                    displayName = displayName,
                    nowPage = nowPage,
                    totalPages = totalPages,
                ))
            }
        }
    }
    private suspend fun ReplyAble.showCharts(
        type: String,
        keyword: String,
        result: List<ChartInfo>,
        displayName: String ?= null,
        nowPage: Int = 1,
        totalPages: Int = 1,
    ) {
        when {
            result.isEmpty() -> {
                reply(notFound)
            }
            result.size == 1 && totalPages == 1 -> {
                reply(result.first().infoText(), result.first().infoMD(jacketUrl))
            }
            else -> {
                val hint = if (totalPages != 1)
                    "您要查找的歌曲可能是 ($nowPage / $totalPages)："
                else
                    "您要查找的歌曲可能是："
                reply(buildString {
                    appendLine(hint)
                    result.forEach { chart ->
                        appendLine("${chart.difficulty.brief}${chart.music.id}. ${chart.music.name}")
                    }
                }.trim(), MarkdownTemplates.Templates.selectChart(
                    title = hint,
                    type = type,
                    keyword = keyword,
                    result = result,
                    displayName = displayName,
                    nowPage = nowPage,
                    totalPages = totalPages,
                ))
            }
        }
    }
    private suspend fun ReplyAble.search(
        name: String,
        page: Int = 1
    ) {
        val (result, nowPage, totalPages) = maimai.aliases.search(name)
            .pagination(page, maxResults)
        showMusics(
            "maimai-search-word",
            name,
            result,
            "",
            nowPage,
            totalPages
        )
    }

    private suspend fun ReplyAble.searchLevel(
        begin: Double,
        end: Double,
        page: Int
    ) {
        val (result, nowPage, totalPages) = maimai.charts()
            .filter { it.difficulty != MusicDifficulty.Utage }
            .filter { it.levelValue in begin..end }
            .pagination(page, maxResults)
        showCharts(
            "maimai-search-level",
            "$begin:$end",
            result,
            "",
            nowPage,
            totalPages
        )
    }
    private suspend fun ReplyAble.searchLevelFit(
        begin: Double,
        end: Double,
        page: Int
    ) {
        val (result, nowPage, totalPages) = maimai.charts()
            .filter { it.difficulty != MusicDifficulty.Utage }
            .filter { it.fitLevelValue in begin..end }
            .pagination(page, maxResults)
        showCharts(
            "maimai-search-level-fit",
            "$begin:$end",
            result,
            "",
            nowPage,
            totalPages
        )
    }
    private suspend fun ReplyAble.searchDesigner(
        designer: String,
        page: Int
    ) {
        val (result, nowPage, totalPages) = with(ComboQuery) {
            listOf(designer(designer)).filterCharts(maimai.maimaiData.musics.values)
        }.pagination(page, maxResults)
        showCharts(
            "maimai-search-designer",
            designer,
            result,
            "",
            nowPage,
            totalPages
        )
    }
    private suspend fun ReplyAble.searchVersion(
        version: String,
        page: Int
    ) {
        val (result, nowPage, totalPages) = maimai.musics().filter {
            version in it.version.name
        }.pagination(page, maxResults)
        showMusics(
            "maimai-search-version",
            version,
            result,
            "",
            nowPage,
            totalPages
        )
    }
    private suspend fun ReplyAble.searchArtist(
        artist: String,
        page: Int
    ) {
        val (result, nowPage, totalPages) = maimai.musics().filter {
            artist in it.artist
        }.pagination(page, maxResults)
        showMusics(
            "maimai-search-artist",
            artist,
            result,
            "",
            nowPage,
            totalPages
        )
    }
    private suspend fun MessageEvent.searchRegex(
        raw: String,
        regex: Regex
    ) {
        val result = maimai.musics()
            .filter { regex.find(it.name) != null }
            .take(maxResults)
        showMusics(
            "maimai-search-regex",
            raw,
            result,
            ""
        )
    }
    private suspend fun ReplyAble.searchBPM(
        bpm: Int,
        page: Int
    ) {
        val (result, nowPage, totalPages) = maimai.musics()
            .filter { it.bpm == bpm }
            .pagination(page, maxResults)
        showMusics(
            "maimai-search-bpm",
            "$bpm",
            result,
            "",
            nowPage,
            totalPages
        )
    }
    private suspend fun ReplyAble.searchCombo(
        query: String,
        page: Int = 1
    ): Boolean {
        // TODO: 全面使用 Exception
        val filters = ComboQuery.filters(query) ?: return false
        when (filters.isSingleChartSelected()) {
            true -> {
                val charts = filters.filterCharts(maimai.musics())
                val (result, nowPage, totalPages) = charts.pagination(page, maxResults)
                showCharts("maimai-search-combo", query, result, "", nowPage, totalPages)
            }
            false -> {
                val musics = filters.filterMusics(maimai.musics())
                val (result, nowPage, totalPages) = musics.pagination(page, maxResults)
                showMusics("maimai-search-combo", query, result, "", nowPage, totalPages)
            }
        }
        return true
    }

    private suspend fun MessageEvent.addAlias(
        raw: String
    ) {
        val args = raw.trim().split(" ", limit = 2).filter { it.isNotBlank() }
        if (args.size < 2)
            throw NeedHelpException()
        val (name, alias) = args.take(2)
        if (alias.length >= 32)
            throw IllegalArgsException("别名太长！")
        val music = maimai.aliases.search(name).firstOrNull() ?: throw NotFoundException()
        if (isAdmin()) {
            MaimaiMusicAliasesTable.add(music, alias)
            maimai.aliases.insert(music.id, alias)
            reply("别名添加成功。")
            return
        }
        var votes = MaimaiMusicAliasesTable[music, alias] ?.also { votes ->
            if (votes >= 0)
                throw IllegalOperationException("该别名已存在！")
            if (MaimaiMusicAliasesVoteTable[music, alias, sender.id]) {
                throw IllegalOperationException("您已经投过票啦，还需${-votes}票通过")
            }
        }
        var auditNameWarning = false
        if (votes == null) {
            val auditResult = aliasAudit.audit(music, alias)
            when (auditResult.type) {
                "political" -> throw IllegalOperationException("该别名疑似存在敏感内容，请检查输入")
                "name" -> auditNameWarning = true
                "nsfw" -> throw IllegalOperationException("该别名疑似存在敏感内容，请检查输入")
                "school" -> throw IllegalOperationException("该别名疑似包含具体学校名称，请检查输入")
            }
        }
        MaimaiMusicAliasesVoteTable.vote(music, alias, sender.id)
        MaimaiMusicAliasesTable.vote(music, alias)
        votes ?.let {
            votes += 1
            if (votes >= 0) {
                maimai.aliases.insert(music.id, alias)
                reply("投票成功，该别名已经通过啦")
            } else {
                reply("投票成功，该别名还需${-votes}票通过。") {
                    brief("别名投票", buildString {
                        appendLine("投票成功，该别名还需${-votes}票通过。")
                    })
                    keyboard {
                        row {
                            at("点我投票", "/mai 添加别名 id${music.id} $alias", enter = true)
                        }
                    }
                }
            }
        } ?: run {
            val message = buildString {
                if (auditNameWarning)
                    appendLine("疑似检测到人名，请勿滥用此功能将您的亲朋好友真实姓名加入别名")
                append("别名添加成功，请使用“添加别名 ${music.id} ${alias}”来进行投票，当有3人投票时别名将通过。")
            }
            reply(message) {
                brief("别名投票", buildString {
                    if (auditNameWarning) {
                        appendLine("> 疑似检测到人名，请勿滥用此功能将您的亲朋好友真实姓名加入别名")
                        appendLine()
                    }
                    appendLine("别名添加成功，当有3人投票时别名将通过。")
                    appendLine("其他人可以点击下方按钮，或者发送“添加别名 ${music.id} ${alias}”来投票。")
                })
                keyboard {
                    row {
                        at("点我投票", "/mai 添加别名 id${music.id} $alias", enter = true)
                    }
                }
            }
        }
    }
    private fun String.levelArgs(): Pair<List<Double>, Int> {
        val args = split(" ")
        val levels = args.filter { '.' in it }.mapNotNull { it.toDoubleOrNull() }
        val page =
            if (args.size > 1 && '.' !in args.last())
                args.last().toIntOrNull() ?: 1
            else 1
        return levels to page
    }
    companion object {
        suspend inline fun VfsFile.toPCM(block: suspend (VfsFile) -> Unit) {
            val pcm = FFMpegTask(FFMpegFileType.PCM) {
                input(absolutePath)
                yes()
                forceFormat("s16le")
                audioCodec("pcm_s16le")
                logLevel("warning")
                audioRate("24k")
                audioChannels(1)
            }.result()
            try {
                block(pcm)
            } finally {
                pcm.delete()
            }
        }
    }
}