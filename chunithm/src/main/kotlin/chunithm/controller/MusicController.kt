package xyz.xszq.bot.chunithm.controller

import korlibs.io.file.VfsFile
import korlibs.io.file.std.localCurrentDirVfs
import xyz.xszq.bot.chunithm.Chunithm
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import xyz.xszq.bot.chunithm.component.CosineSimilarity
import xyz.xszq.bot.chunithm.component.CoverDescData
import xyz.xszq.bot.chunithm.component.CoverEmbeddingGenerator
import xyz.xszq.bot.chunithm.component.MarkdownTemplates
import xyz.xszq.bot.chunithm.database.ChunithmMusicAliasesTable
import xyz.xszq.bot.chunithm.database.ChunithmMusicAliasesVoteTable
import xyz.xszq.bot.chunithm.music.ChartInfo
import xyz.xszq.bot.chunithm.music.MusicDifficulty
import xyz.xszq.bot.chunithm.music.MusicInfo
import xyz.xszq.bot.chunithm.query.ComboQuery
import xyz.xszq.bot.chunithm.query.ComboQuery.filterCharts
import xyz.xszq.bot.chunithm.query.ComboQuery.filterMusics
import xyz.xszq.bot.chunithm.query.ComboQuery.isSingleChartSelected
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.event.ReplyAble
import xyz.xszq.bot.exception.IllegalArgsException
import xyz.xszq.bot.exception.IllegalOperationException
import xyz.xszq.bot.exception.NeedHelpException
import xyz.xszq.bot.exception.NotFoundException
import xyz.xszq.bot.ffmpeg.FFMpegFileType
import xyz.xszq.bot.ffmpeg.FFMpegTask
import xyz.xszq.bot.message.Audio
import xyz.xszq.bot.pagination
import xyz.xszq.bot.reply
import xyz.xszq.bot.subscribe.CommandNotMatchedException
import kotlin.random.Random

@Suppress("unused")
class MusicController(
    override val chunithm: Chunithm
): Controller(chunithm) {
    private val previewDir = "${chunithm.dataPath}/preview/"
    private val notFound = "未查找到相关的歌曲，请检查拼写是否有误。"
    private val maxResults = 10

    private val jacketUrl = chunithm.config.tokens["assets-jacket"] ?: throw Exception("assets-jacket missing")

    private var coverEmbeddings: Map<Int, FloatArray>? = null
    private var coverDescriptions: Map<Int, CoverDescData>? = null
    private val coverEmbeddingsPath = "${chunithm.dataPath}/cover-embeddings.json"
    private val coverDescriptionsPath = "${chunithm.dataPath}/cover-descriptions.json"
    private val llmJson = Json { ignoreUnknownKeys = true }

    override suspend fun setRoute() = rhythm {
        startsWith("id") { raw ->
            val id = raw.toIntOrNull() ?: throw CommandNotMatchedException()
            val music = chunithm.music(id) ?: throw CommandNotMatchedException()
            reply(music.infoText(), music.infoMD(jacketUrl))
        }
        MusicDifficulty.entries.forEach { difficulty ->
            val name = difficulty.brief
            startsWith(listOf("${name}id", name)) { raw ->
                val id = raw.toIntOrNull() ?: throw CommandNotMatchedException()
                val music = chunithm.music(id) ?: throw CommandNotMatchedException()
                val chart = music.charts.firstOrNull { it.difficulty == difficulty } ?: throw CommandNotMatchedException()
                reply(chart.infoText(), chart.infoMD(jacketUrl))
            }
        }

        startsWith("查歌") { raw ->
            queryByTextOrImage(raw) { search(it) }
        }
        endsWith(listOf("是什么歌", "是什么歌？")) { raw ->
            queryByTextOrImage(raw) { search(it) }
        }
        button("chunithm-search-word") {
            val args = data.split("\n", limit = 2)
            search(args[0], args[1].toInt())
        }

        startsWith("定数查歌") { raw ->
            val (levels, page) = raw.levelArgs()
            when {
                levels.size == 1 -> searchLevel(levels[0], levels[0], page)
                levels.size >= 2 -> searchLevel(levels[0], levels[1], page)
                else -> reply(buildString {
                    appendLine("使用方法：定数查歌 [定数] [定数] [页数]")
                    appendLine("例：定数查歌 13.0")
                    appendLine("例：定数查歌 13.4 13.8")
                    appendLine("例：定数查歌 12.2 12.5 2")
                })
            }
        }
        button("chunithm-search-level") {
            val args = data.split("\n", limit = 2)
            val (begin, end) = args[0].split(":").map { it.toDouble() }
            searchLevel(begin, end, args[1].toInt())
        }

        startsWith("谱师查歌") { name ->
            when {
                name.isNotBlank() -> searchDesigner(name, 1)
                else -> reply(buildString {
                    appendLine("使用方法：谱师查歌 [名称]")
                    appendLine("例：谱师查歌 翠楼屋")
                    appendLine("例：谱师查歌 mai-Star")
                })
            }
        }
        button("chunithm-search-designer") {
            val args = data.split("\n", limit = 2)
            searchDesigner(args[0], args[1].toInt())
        }

        startsWith("版本查歌") { name ->
            when {
                name.isNotBlank() -> searchVersion(name, 1)
                else -> reply(buildString {
                    appendLine("使用方法：版本查歌 [版本名]")
                    appendLine("例：版本查歌 STAR PLUS")
                    appendLine("例：版本查歌 SUN")
                })
            }
        }
        button("chunithm-search-version") {
            val args = data.split("\n", limit = 2)
            searchVersion(args[0], args[1].toInt())
        }

        startsWith("曲师查歌") { name ->
            when {
                name.isNotBlank() -> searchArtist(name, 1)
                else -> reply(buildString {
                    appendLine("使用方法：曲师查歌 [曲师名]")
                    appendLine("例：曲师查歌 t+pazolite")
                    appendLine("例：曲师查歌 豚乙女")
                })
            }
        }
        button("chunithm-search-artist") {
            val args = data.split("\n", limit = 2)
            searchArtist(args[0], args[1].toInt())
        }

        startsWith("正则查歌") { raw ->
            if (raw.isBlank()) {
                reply(buildString {
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
        button("chunithm-search-bpm") {
            val args = data.split("\n", limit = 2)
            searchBPM(args[0].toInt(), args[1].toInt())
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
        button("chunithm-search-combo") {
            val args = data.split("\n", limit = 2)
            val query = args[0]
            val page = args[1].toInt()
            if (!searchCombo(query, page)) {
                reply("未找到相关歌曲。")
            }
        }
        startsWith("随个") { raw ->
            val result = searchMusic(raw)
            if (result.isEmpty()) {
                reply(notFound)
                return@startsWith
            }
            reply(result.random(Random(System.currentTimeMillis())).infoText())
        }
        endsWith(listOf("有什么别名", "有什么别名？")) { name ->
            queryByTextOrImage(name) {
                val music = chunithm.aliases.search(it).firstOrNull() ?: return@queryByTextOrImage
                val aliases = ChunithmMusicAliasesTable[music]
                    .filter { a -> a.first != music.name }
                    .take(40)
                    .joinToString("\n") { (alias, _) -> alias }
                val text = buildString {
                    appendLine("${music.id}.${music.name} 有如下别名：")
                    appendLine(aliases)
                    appendLine()
                    appendLine("可以@机器人使用“添加别名 id 别名”来添加别名。")
                }.trim()
                reply(text) {
                    brief("别名列表", text)
                    keyboard {
                        row {
                            at("添加别名", "/chu 添加别名 id${music.id}")
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
                    appendLine(" 例：添加别名 2579 祝福荣光")
                }.trim()
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
            val music = chunithm.aliases.search(name).firstOrNull() ?: run {
                reply("未找到该歌曲。")
                return@startsWith
            }
            val existing = ChunithmMusicAliasesTable[music, alias]
            if (existing == null) {
                reply("该别名不存在。")
                return@startsWith
            }
            ChunithmMusicAliasesTable.remove(music, alias)
            chunithm.aliases.delete(music.id, alias)
            reply("别名已删除。")
        }
        startsWith("预览") { musicQuery ->
            queryByTextOrImage(musicQuery) {
                val (music, _) = selectMusic("预览", it, false)
                    ?: return@queryByTextOrImage
                val file = localCurrentDirVfs[previewDir]["${music.resourceId}.ogg"]
                if (!file.exists()) {
                    return@queryByTextOrImage
                }
                file.toPCM { pcm ->
                    reply(Audio(pcm))
                }
            }
        }

        startsWith("生成封面embedding") {
            if (!isAdmin()) {
                return@startsWith
            }
            val client = bot.pluginLoader.llmClient ?: return@startsWith
            chunithm.scope.launch {
                try {
                    CoverEmbeddingGenerator.generate(
                        client = client,
                        coversDir = MusicInfo.coverDir,
                        outputPath = coverEmbeddingsPath,
                    )
                    coverEmbeddings = CoverEmbeddingGenerator.load(coverEmbeddingsPath)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        startsWith("生成封面描述") {
            if (!isAdmin()) {
                return@startsWith
            }
            val client = bot.pluginLoader.llmClient ?: return@startsWith
            chunithm.scope.launch {
                try {
                    CoverEmbeddingGenerator.generateDescriptions(
                        client = client,
                        coversDir = MusicInfo.coverDir,
                        outputPath = coverDescriptionsPath,
                    )
                    coverDescriptions = CoverEmbeddingGenerator.loadDescriptions(coverDescriptionsPath)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        startsWith("帮我找") { query ->
            if (query.isBlank()) {
                reply("使用方法：帮我找 [封面描述]\n例：帮我找像素小人")
                return@startsWith
            }
            reply("正在搜索中……")
            searchByCover(query)
        }
    }

    @Serializable
    data class CoverIdsResult(
        val ids: List<Int> = emptyList(),
    )

    @Serializable
    data class CoverQueriesResult(
        val queries: List<String> = emptyList(),
    )

    private suspend fun ReplyAble.searchByCover(query: String) {
        val client = bot.pluginLoader.llmClient ?: return
        if (coverEmbeddings == null) {
            coverEmbeddings = CoverEmbeddingGenerator.load(coverEmbeddingsPath)
        }
        if (coverEmbeddings.isNullOrEmpty()) {
            return
        }
        if (coverDescriptions == null) {
            coverDescriptions = CoverEmbeddingGenerator.loadDescriptions(coverDescriptionsPath)
        }
        try {
            val decomposition = client.chat(scene = "rhythm-game") {
                responseFormat("json_object")
                system("你是一个中二节奏封面搜索助手。用户的描述可能包含多个视觉特征，请拆成独立的短查询。每个短查询只描述一个视觉特征。\n以JSON格式返回：{\"queries\": [\"特征1\", \"特征2\"]}\n例：\"黄色背景戴帽子的男的粉蓝色头发比了一个圆\" → {\"queries\": [\"黄色背景\", \"戴帽子的男角色\", \"粉蓝色头发\", \"比了一个圆\"]}\n不要遗漏任何特征。")
                user(query)
            }
            val subQueries = try {
                llmJson.decodeFromString<CoverQueriesResult>(decomposition).queries
            } catch (_: Exception) {
                emptyList()
            }
            val allQueries = listOf(query) + (subQueries.take(5))
            val votes = mutableMapOf<Int, Int>()
            val maxSim = mutableMapOf<Int, Double>()
            for (subQuery in allQueries) {
                if (subQuery.isBlank()) continue
                val qv = client.embed(scene = "embedding", input = subQuery)
                if (qv.isEmpty()) continue
                val qvArray = qv.toFloatArray()
                val fused = coverEmbeddings!!.mapValues { (resourceId, imgVec) ->
                    val descVec = coverDescriptions?.get(resourceId)?.vec
                    val imgSim = CosineSimilarity.compute(qvArray, imgVec)
                    val descSim = if (descVec != null) CosineSimilarity.compute(qvArray, descVec) else imgSim
                    imgSim * 0.6 + descSim * 0.4
                }
                fused.entries.sortedByDescending { it.value }.take(20).forEach { (rid, score) ->
                    votes[rid] = (votes[rid] ?: 0) + 1
                    val prev = maxSim[rid] ?: -1.0
                    if (score > prev) maxSim[rid] = score
                }
            }
            val ranked = votes.entries.sortedByDescending { (rid) ->
                votes[rid]!! * 10000 + (maxSim[rid] ?: 0.0).toLong()
            }.take(30)
            if (ranked.isEmpty()) {
                reply("没有找到匹配的歌曲")
                return
            }
            val candidates = ranked.mapNotNull { (resourceId, _) ->
                val music = chunithm.musics().firstOrNull { it.resourceId == resourceId }
                music?.let { it to (votes[resourceId] ?: 0) }
            }
            if (candidates.isEmpty()) {
                reply("没有找到匹配的歌曲")
                return
            }
            val candidateInfo = candidates.joinToString("\n") { (music, voteCount) ->
                val desc = coverDescriptions?.get(music.resourceId)?.desc?.take(200) ?: ""
                buildString {
                    appendLine("${music.id}. ${music.name}")
                    appendLine("   艺术家: ${music.artist} | 分类: ${music.genre.genreName} | 版本: ${music.version.name}")
                    if (desc.isNotBlank()) appendLine("   封面描述: $desc")
                    appendLine("   命中子查询: $voteCount/${allQueries.size}")
                }
            }
            val systemPrompt = buildString {
                appendLine("你是一个中二节奏歌曲搜索助手。")
                appendLine("用户用自然语言描述了他记忆中的歌曲封面特征。")
                appendLine("以下是候选歌曲及其详细信息：")
                appendLine(candidateInfo)
                appendLine()
                appendLine("请根据用户的描述，综合封面描述、歌曲信息（艺术家、分类、版本等）选出最匹配的歌曲ID。")
                appendLine("如果某个封面明显匹配，只返回1首。如果多个候选都类似，最多返回5首。")
                appendLine("完全不匹配则返回空数组。只返回JSON，不要其他内容：")
                appendLine("{\"ids\": [数字]} // 举例: {\"ids\": [299]} 或 {\"ids\": [299, 300, 301]}")
            }
            val rankingResult = client.chat(scene = "rhythm-game") {
                responseFormat("json_object")
                system(systemPrompt)
                user(query)
            }
            val resultIds = llmJson.decodeFromString<CoverIdsResult>(rankingResult).ids
            val musics = resultIds.mapNotNull { chunithm.music(it) }
            if (musics.isEmpty()) {
                reply("没有找到匹配的歌曲")
                return
            }
            showMusics("chunithm-cover-search", query, musics, "", 1, 1)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun searchMusic(
        name: String
    ): List<MusicInfo> {
        val query = name.trim()
        if (query.isBlank())
            return chunithm.musics().sortedBy { music -> music.id }
        return chunithm.aliases.search(query)
    }

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
                }.trim(), MarkdownTemplates.Templates.selectMusic(
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
        val (result, nowPage, totalPages) = searchMusic(name).pagination(page, maxResults)
        if (result.isEmpty()) {
            reply("未找到结果，正在根据封面特征查找……\n\nTIPS：使用\"帮我找\"命令可以直接根据封面特征查找") {
                line("**中二节奏**")
                line("未找到结果，正在根据封面特征查找……")
                line()
                line("> TIPS：使用\"帮我找\"命令可以直接根据封面特征查找")
            }
            searchByCover(name)
            return
        }
        showMusics(
            "chunithm-search-word",
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
        val (result, nowPage, totalPages) = chunithm.charts()
            .filter { it.difficulty != MusicDifficulty.WorldsEnd }
            .filter { it.levelValue in begin..end }
            .pagination(page, maxResults)
        showCharts(
            "chunithm-search-level",
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
            listOf(designer(designer)).filterCharts(chunithm.chunithmData.musics.values)
        }.pagination(page, maxResults)
        showCharts(
            "chunithm-search-designer",
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
        val (result, nowPage, totalPages) = chunithm.musics()
            .filter { version == it.version.name || version in it.version.name }
            .pagination(page, maxResults)
        showMusics(
            "chunithm-search-version",
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
        val (result, nowPage, totalPages) = chunithm.musics()
            .filter { artist in it.artist }
            .pagination(page, maxResults)
        showMusics(
            "chunithm-search-artist",
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
        val result = chunithm.musics()
            .filter { regex.find(it.name) != null }
            .take(maxResults)
        showMusics(
            "chunithm-search-regex",
            raw,
            result,
            ""
        )
    }

    private suspend fun ReplyAble.searchBPM(
        bpm: Int,
        page: Int
    ) {
        val (result, nowPage, totalPages) = chunithm.musics()
            .filter { it.bpm == bpm }
            .pagination(page, maxResults)
        showMusics(
            "chunithm-search-bpm",
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
                val charts = filters.filterCharts(chunithm.musics())
                val (result, nowPage, totalPages) = charts.pagination(page, maxResults)
                showCharts("chunithm-search-combo", query, result, "", nowPage, totalPages)
            }
            false -> {
                val musics = filters.filterMusics(chunithm.musics())
                val (result, nowPage, totalPages) = musics.pagination(page, maxResults)
                showMusics("chunithm-search-combo", query, result, "", nowPage, totalPages)
            }
        }
        return true
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
    private suspend fun MessageEvent.addAlias(raw: String) {
        val args = raw.trim().split(" ", limit = 2).filter { it.isNotBlank() }
        if (args.size < 2) throw NeedHelpException()
        val (name, alias) = args.take(2)
        if (alias.length >= 32) throw IllegalArgsException("别名太长！")
        val music = chunithm.aliases.search(name).firstOrNull() ?: throw NotFoundException()
        if (isAdmin()) {
            ChunithmMusicAliasesTable.add(music, alias)
            chunithm.aliases.insert(music.id, alias)
            reply("别名已添加。")
            return
        }
        val existing = ChunithmMusicAliasesTable[music, alias]
        if (existing != null) {
            if (existing >= 0)
                throw IllegalOperationException("该别名已存在！")
            if (ChunithmMusicAliasesVoteTable[music, alias, sender.id]) {
                throw IllegalOperationException("您已经投过票啦，还需${-existing}票通过")
            }
        }
        ChunithmMusicAliasesVoteTable.vote(music, alias, sender.id)
        ChunithmMusicAliasesTable.vote(music, alias)
        val current = existing?.let { it + 1 } ?: -2
        if (existing != null) {
            if (current >= 0) {
                chunithm.aliases.insert(music.id, alias)
                reply("投票成功，该别名已经通过啦")
            } else {
                reply("投票成功，该别名还需${-current}票通过。") {
                    brief("别名投票", buildString {
                        appendLine("投票成功，该别名还需${-current}票通过。")
                    })
                    keyboard {
                        row {
                            at("点我投票", "/chu 添加别名 id${music.id} $alias", enter = true)
                        }
                    }
                }
            }
        } else {
            reply("别名添加成功，请使用“添加别名 ${music.id} ${alias}”来进行投票，当有3人投票时别名将通过。") {
                brief("别名投票", buildString {
                    appendLine("别名添加成功，当有3人投票时别名将通过。")
                    appendLine("其他人可以点击下方按钮，或者发送“添加别名 ${music.id} ${alias}”来投票。")
                })
                keyboard {
                    row {
                        at("点我投票", "/chu 添加别名 id${music.id} $alias", enter = true)
                    }
                }
            }
        }
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