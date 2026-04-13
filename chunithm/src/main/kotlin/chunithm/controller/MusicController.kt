package xyz.xszq.bot.chunithm.controller

import xyz.xszq.bot.Chunithm
import xyz.xszq.bot.chunithm.music.ChartInfo
import xyz.xszq.bot.chunithm.music.MusicDifficulty
import xyz.xszq.bot.chunithm.music.MusicInfo
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.event.ReplyAble
import xyz.xszq.bot.pagination
import xyz.xszq.bot.reply
import kotlin.random.Random

@Suppress("unused")
class MusicController(
    override val chunithm: Chunithm
): Controller(chunithm) {
    private val notFound = "未查找到相关的歌曲，请检查拼写是否有误。"
    private val maxResults = 10

    override suspend fun setRoute() = rhythm {
        startsWith("随个") { raw ->
            val result = searchMusic(raw)
            if (result.isEmpty()) {
                reply(notFound)
                return@startsWith
            }
            reply(result.random(Random(System.currentTimeMillis())).infoText())
        }
        startsWith("id") { raw ->
            val id = raw.toIntOrNull() ?: return@startsWith
            val music = chunithm.music(id) ?: return@startsWith
            reply(music.infoText())
        }
        MusicDifficulty.entries.forEach { difficulty ->
            val name = difficulty.brief
            startsWith(listOf("${name}id", name)) { raw ->
                val id = raw.toIntOrNull() ?: return@startsWith
                val music = chunithm.music(id) ?: return@startsWith
                val chart = music.charts.firstOrNull { it.difficulty == difficulty } ?: return@startsWith
                reply(chart.infoText())
            }
        }

        startsWith("查歌") { raw ->
            val args = raw.split("\n", limit = 2)
            search(args[0])
        }
        endsWith(listOf("是什么歌", "是什么歌？")) { raw ->
            val args = raw.split("\n", limit = 2)
            search(args[0])
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
        result: List<MusicInfo>,
        nowPage: Int = 1,
        totalPages: Int = 1
    ) {
        when {
            result.isEmpty() -> reply(notFound)
            result.size == 1 && totalPages == 1 -> reply(result.first().infoText())
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
                }.trim())
            }
        }
    }

    private suspend fun ReplyAble.showCharts(
        result: List<ChartInfo>,
        nowPage: Int = 1,
        totalPages: Int = 1
    ) {
        when {
            result.isEmpty() -> reply(notFound)
            result.size == 1 && totalPages == 1 -> reply(result.first().infoText())
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
                }.trim())
            }
        }
    }

    private suspend fun ReplyAble.search(
        name: String,
        page: Int = 1
    ) {
        val (result, nowPage, totalPages) = searchMusic(name).pagination(page, maxResults)
        showMusics(result, nowPage, totalPages)
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
        showCharts(result, nowPage, totalPages)
    }

    private suspend fun ReplyAble.searchDesigner(
        designer: String,
        page: Int
    ) {
        val (result, nowPage, totalPages) = chunithm.charts()
            .filter { designer == it.notesDesigner || designer in it.notesDesigner }
            .pagination(page, maxResults)
        showCharts(result, nowPage, totalPages)
    }

    private suspend fun ReplyAble.searchVersion(
        version: String,
        page: Int
    ) {
        val (result, nowPage, totalPages) = chunithm.musics()
            .filter { version == it.version.name || version in it.version.name }
            .pagination(page, maxResults)
        showMusics(result, nowPage, totalPages)
    }

    private suspend fun ReplyAble.searchArtist(
        artist: String,
        page: Int
    ) {
        val (result, nowPage, totalPages) = chunithm.musics()
            .filter { artist in it.artist }
            .pagination(page, maxResults)
        showMusics(result, nowPage, totalPages)
    }

    private suspend fun MessageEvent.searchRegex(
        raw: String,
        regex: Regex
    ) {
        val result = chunithm.musics()
            .filter { regex.find(it.name) != null }
            .take(maxResults)
        showMusics(result)
    }

    private suspend fun ReplyAble.searchBPM(
        bpm: Int,
        page: Int
    ) {
        val (result, nowPage, totalPages) = chunithm.musics()
            .filter { it.bpm == bpm }
            .pagination(page, maxResults)
        showMusics(result, nowPage, totalPages)
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
}