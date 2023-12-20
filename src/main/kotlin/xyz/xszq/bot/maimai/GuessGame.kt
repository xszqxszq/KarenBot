package xyz.xszq.bot.maimai

import korlibs.image.format.PNG
import korlibs.image.format.encode
import korlibs.image.format.readNativeImage
import korlibs.image.text.HorizontalAlign
import korlibs.image.text.VerticalAlign
import korlibs.io.file.std.localCurrentDirVfs
import korlibs.math.roundDecimalPlaces
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import xyz.xszq.bot.image.BuildImage
import xyz.xszq.bot.image.randomSlice
import xyz.xszq.bot.image.toBuildImage
import xyz.xszq.bot.maimai.MaimaiUtils.toDXId
import xyz.xszq.bot.maimai.payload.ChartStat
import xyz.xszq.bot.maimai.payload.MusicInfo
import xyz.xszq.nereides.event.GlobalEventChannel
import xyz.xszq.nereides.event.PublicMessageEvent
import xyz.xszq.nereides.message.Ark
import xyz.xszq.nereides.message.ark.ListArk
import xyz.xszq.nereides.message.plus
import xyz.xszq.nereides.message.toImage
import xyz.xszq.nereides.message.toPlainText
import xyz.xszq.nereides.toDBC

class GuessGame(
    private val musics: MusicsInfo,
    private val images: MaimaiImage,
    private val aliases: Aliases
) {

    private val cooldown = 10000L
    private val started = mutableMapOf<String, Boolean>()
    private fun getDescriptions(song: MusicInfo, stat: List<ChartStat>) = listOf(
        "的版本为 ${song.basicInfo.from}${if (song.basicInfo.isNew) " (计入b15)" else ""}",
        "的艺术家为 ${song.basicInfo.artist}",
        "的分类为 ${song.basicInfo.genre}",
        "的 BPM 为 ${song.basicInfo.bpm}",
        "的红谱等级为 ${song.level[2]}，查分器拟合定数为${stat[2].fitDiff!!.roundDecimalPlaces(1)}",
        "的紫谱等级为 ${song.level[3]}，查分器拟合定数为${stat[3].fitDiff!!.roundDecimalPlaces(1)}",
        "的紫谱谱师为 ${song.charts[3].charter}",
        "${if (song.level.size == 4) "没有" else "有"}白谱",
        if (song.type=="DX") "是 DX 谱面" else if (musics
                .getById(toDXId(song.id)) != null) "既有 DX 谱面也有标准谱面" else "没有 DX 谱面"
    )
    suspend fun startClassical(event: PublicMessageEvent, hints: Int = 6) = event.run {
        if (started[contextId] == true) {
            reply("当前群有正在进行的猜歌/开字母哦~\n输入”不玩了“可以结束上一局")
            return@run
        }
        started[contextId] = true


        val music = musics.getRandomHot()
        val stat = musics.getStats(music.id)!!

        val options = if (!images.getCoverById(music.id).exists()) hints else hints + 1
        val descriptions = getDescriptions(music, stat).shuffled().take(hints).mapIndexed { i, desc ->
            "提示${i + 1}/$options：这首歌$desc"
        }.toMutableList()
        val ansList = mutableListOf(music.id, music.title)
        ansList.addAll(aliases.getAllAliases(music.id))
        if (music.type == "SD" && musics.getById(toDXId(music.id)) != null) {
            ansList.add(toDXId(music.id))
            ansList.addAll(aliases.getAllAliases(toDXId(music.id)))
        }
        ansList.replaceAll { it.lowercase() }
        println(ansList)
        reply("请各位发挥自己的聪明才智，根据我的提示来猜一猜这是哪一首歌曲吧！记得要 @可怜Bot 作答哦~\n" +
                "作答时，歌曲 id、歌曲标题（请尽量回答完整）、歌曲别名都将被视作有效答案哦~\n" +
                "(致管理员：您可以使用“/mai 设置猜歌”指令开启或者关闭本群的猜歌功能)")
        var finished = false
        coroutineScope {
            launch {
                GlobalEventChannel.channel.takeWhile { !finished }.collect { e ->
                    if (e !is PublicMessageEvent ||
                                e.contextId != contextId)
                        return@collect
                    if (e.message.text == "不玩了") {
                        finished = true
                        started[contextId] = false
                        return@collect
                    }
                    if (!(ansList.any { ans -> ans == e.contentString.lowercase() ||
                                ans in e.contentString.lowercase()
                                || (e.contentString.length >= 5 && e.contentString.lowercase() in ans)}))
                        return@collect
                    val replyText = buildString {
                        appendLine("恭喜您猜中了哦~")
                        append(musics.getInfo(music.id).trim())
                    }
                    val cover = images.getCoverById(music.id)
                    if (cover.exists())
                        e.reply(replyText.toPlainText() + cover.toImage())
                    else
                        e.reply(replyText)
                    finished = true
                    started[contextId] = false
                }
            }
            launch {
                delay(cooldown)
                descriptions.forEachIndexed { index, desc ->
                    if (finished)
                        return@launch
                    reply(buildString {
                        appendLine()
                        appendLine(desc)
                        if (index == options - 1)
                            appendLine("30秒后将揭晓答案哦~")
                    }.trimEnd())
                    delay(cooldown)
                }
                if (finished)
                    return@launch
                if (options == descriptions.size + 1) {
                    reply(
                        buildString {
                            appendLine()
                            appendLine("这首歌的封面部分如图，30秒后将揭晓答案哦~")
                        }.trimEnd().toPlainText() + images.getCoverBitmap(music.id).randomSlice().encode(PNG).toImage()
                    )
                }
                delay(30000L)
                if (finished)
                    return@launch
                val replyText = buildString {
                    appendLine("很遗憾，没有人猜中哦")
                    append(musics.getInfo(music.id))
                }
                val cover = images.getCoverById(music.id)
                if (cover.exists())
                    reply(replyText.toPlainText() + cover.toImage())
                else
                    reply(replyText)
                finished = true
                started[contextId] = false
            }
        }
    }
    private suspend fun showOpeningStatus(
        nowMusics: List<Pair<MusicInfo, Boolean>>,
        nowChars: List<Char>,
        all: Boolean = false
    ): Ark = ListArk.build {
        desc { "舞萌开字母猜歌小游戏" }
        prompt { "舞萌开字母" }
        text { "已开出字母：[${nowChars.joinToString(", ")}]" }
        text { "题目如下：" }

        nowMusics.forEach { (music, status) ->
            if (status) {
                text { "[√]: ${music.title}" }
            } else {
                text { "[X]: " + music.title.map { c ->
                    if (c.lowercase().toDBC().first() !in nowChars && c.toString().isNotBlank() && !all) '?' else c
                }.joinToString("") }
            }
        }
    }
    suspend fun startOpening(event: PublicMessageEvent) = event.run {
        if (started[contextId] == true) {
            reply("当前群有正在进行的猜歌/开字母哦~\n输入”不玩了“可以结束上一局")
            return@run
        }
        started[contextId] = true
        var finished = false
        val nowMusics = musics.getRandomHot(15).map { Pair(it, false) }.toMutableList()
        println(nowMusics.map { it.first.id })
        val nowChars = mutableListOf<Char>()
        reply(ListArk.build {
            desc { "舞萌开字母猜歌小游戏" }
            prompt { "舞萌开字母" }
            text { "这是一个 maimai 猜歌小游戏~" }
            text { "你需要猜出十五首来自 maimai 的歌曲曲名！可以@可怜Bot说：“开字母”来尝试开一次字母，说“开歌”来直接开出歌曲，说“不玩了”可以结束游戏哦" }
        })
        reply("\n")
        coroutineScope {
            launch {
                reply(showOpeningStatus(nowMusics, nowChars))
                GlobalEventChannel.channel.takeWhile { !finished }.collect { e ->
                    kotlin.runCatching {
                        if (e !is PublicMessageEvent ||
                            e.contextId != contextId)
                            return@collect
                        if (e.message.text == "不玩了") {
                            finished = true
                            started[contextId] = false
                            e.reply(showOpeningStatus(nowMusics, nowChars, true))
                            return@collect
                        }
                        if (e.message.text.trim().startsWith("开字母")) {
                            val char = e.message.text.trim().substringAfter("开字母").trim().firstOrNull()
                                ?: return@collect
                            if (char in nowChars) {
                                e.reply(ListArk.build {
                                    desc { "舞萌开字母猜歌小游戏" }
                                    prompt { "舞萌开字母" }
                                    text { "字母“${char}”已经开过了！" }
                                })
                                return@collect
                            }
                            nowChars.add(char.lowercase().toDBC().first())
                            nowMusics.forEachIndexed { index, music ->
                                if (!music.second && music.first.title.all { it.lowercase().toDBC().first() in nowChars })
                                    nowMusics[index] = Pair(music.first, true)
                            }
                        } else if (e.message.text.trim().startsWith("开歌")) {
                            val name = e.message.text.trim().substringAfter("开歌").trim().lowercase().toDBC()
                            var musicIndex: Int? = null
                            var notFound = true
                            musics.getById(name.substringAfter("id")) ?.let { music ->
                                notFound = false
                                if (nowMusics.any { it.first.title == music.title })
                                    musicIndex = nowMusics.indexOfFirst { it.first.title == music.title }
                            }
                            musics.filter { it.title == name }.firstOrNull() ?.let { music ->
                                notFound = false
                                if (nowMusics.any { it.first.title == music.title })
                                    musicIndex = nowMusics.indexOfFirst { it.first.title == music.title }
                            }
                            aliases.findByAlias(name).forEach { music ->
                                notFound = false
                                nowMusics.mapIndexedNotNull { index, now ->
                                    if (!now.second && now.first.title == music.title)
                                        index
                                    else null
                                }.firstOrNull() ?.let {
                                    musicIndex = it
                                }
                            }
                            musicIndex ?.let {
                                nowMusics[it] = Pair(nowMusics[it].first, true)
                            } ?: run {
                                e.reply(ListArk.build {
                                    desc { "舞萌开字母猜歌小游戏" }
                                    prompt { "舞萌开字母" }
                                    text { if (notFound) "歌曲不存在！" else "歌曲不在题目列表中！" }
                                })
                                return@collect
                            }
                        } else {
                            return@collect
                        }
                        if (nowMusics.all { it.second }) {
                            e.reply(ListArk.build {
                                desc { "舞萌开字母猜歌小游戏" }
                                prompt { "舞萌开字母" }
                                text { "恭喜！全部歌曲已开出" }
                            })
                            e.reply(showOpeningStatus(nowMusics, nowChars))
                            finished = true
                            started[contextId] = false
                            return@collect
                        }
                        e.reply(showOpeningStatus(nowMusics, nowChars))
                    }.onFailure {
                        it.printStackTrace()
                    }
                }
            }
        }
    }
}