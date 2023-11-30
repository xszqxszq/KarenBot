package xyz.xszq.bot.maimai

import com.soywiz.korim.format.PNG
import com.soywiz.korim.format.encode
import com.soywiz.korio.async.launch
import com.soywiz.korma.math.roundDecimalPlaces
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.takeWhile
import xyz.xszq.bot.maimai.MaimaiUtils.toDXId
import xyz.xszq.bot.maimai.payload.ChartStat
import xyz.xszq.bot.maimai.payload.MusicInfo
import xyz.xszq.nereides.event.GlobalEventChannel
import xyz.xszq.nereides.event.GroupAtMessageEvent
import xyz.xszq.nereides.message.plus
import xyz.xszq.nereides.message.toImage
import xyz.xszq.nereides.message.toPlainText
import xyz.xszq.nereides.pass
import kotlin.coroutines.CoroutineContext

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
    suspend fun start(event: GroupAtMessageEvent, hints: Int = 6) = event.run {
        if (started[contextId] == true) {
            reply("当前群有正在进行的猜歌哦~")
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
        reply("请各位发挥自己的聪明才智，根据我的提示来猜一猜这是哪一首歌曲吧！\n" +
                "作答时，歌曲 id、歌曲标题（请尽量回答完整）、歌曲别名都将被视作有效答案哦~\n" +
                "(致管理员：您可以使用“/mai 设置猜歌”指令开启或者关闭本群的猜歌功能)")
        var finished = false
        coroutineScope {
            val job1 = launch {
                GlobalEventChannel.channel.collect { e ->
                    if (e !is GroupAtMessageEvent ||
                                e.contextId != contextId)
                        return@collect
                    println(e.contentString)
                    if (!(ansList.any { ans -> ans == e.contentString.lowercase() ||
                                ans in e.contentString.lowercase()
                                || (e.contentString.length >= 5 && e.contentString.lowercase() in ans)}))
                        return@collect
                    val replyText = buildString {
                        appendLine("恭喜您猜中了哦~")
                        append(musics.getInfo(music.id))
                    }
                    val cover = images.getCoverById(music.id)
                    if (cover.exists())
                        e.reply(replyText.toPlainText() + cover.toImage())
                    else
                        e.reply(replyText)
                    finished = true
                    return@collect
                }
            }
            val job2 = launch {
                descriptions.forEachIndexed { index, desc ->
                    reply(buildString {
                        appendLine()
                        appendLine(desc)
                        if (index == options - 1)
                            appendLine("30秒后将揭晓答案哦~")
                    }.trimEnd())
                    delay(cooldown)
                }
                if (options == descriptions.size + 1) {
                    reply(
                        buildString {
                            appendLine()
                            appendLine("这首歌的封面部分如图，30秒后将揭晓答案哦~")
                        }.trimEnd() + images.getCoverBitmap(music.id).randomSlice().encode(PNG).toImage()
                    )
                }
                delay(30000L)
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
            }
            launch {
                while (!finished) {
                    delay(100L)
                }
                if (job1.isActive)
                    job1.cancel()
                if (job2.isActive)
                    job2.cancel()
            }
        }

        started[contextId] = false
    }
}