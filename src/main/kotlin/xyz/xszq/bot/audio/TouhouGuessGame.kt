package xyz.xszq.bot.audio

import korlibs.io.file.std.localCurrentDirVfs
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.select
import xyz.xszq.bot.dao.TouhouAliases
import xyz.xszq.bot.dao.TouhouMusics
import xyz.xszq.bot.dao.transactionWithLock
import xyz.xszq.nereides.event.BotRestartEvent
import xyz.xszq.nereides.event.GlobalEventChannel
import xyz.xszq.nereides.event.GroupAtMessageEvent
import xyz.xszq.nereides.event.PublicMessageEvent
import xyz.xszq.nereides.message.toVoice
import xyz.xszq.nereides.toPinyinAbbr
import xyz.xszq.nereides.useTempFile
import kotlin.random.Random

object TouhouGuessGame {
    private val started = mutableMapOf<String, Boolean>()
    private val musicDir = localCurrentDirVfs["music/touhou"]
    enum class Difficulty {
        Easy, Normal, Hard, Lunatic, Extra
    }
    enum class Range {
        BeforeKishinjou, AfterShinkirou, STGOnly, FTGOnly, AllNew, Old, HifuuOnly
    }
    private fun getTime(diff: Difficulty) = when (diff) {
        Difficulty.Easy -> 10.0
        Difficulty.Normal -> 5.0
        Difficulty.Hard -> 2.0
        Difficulty.Lunatic -> 1.0
        Difficulty.Extra -> 2.0
    }
    private fun rangeToList(range: Range) = when (range) {
        Range.BeforeKishinjou -> { // TODO: 小数点作
            (6..14).map { id -> "th%02d".format(id) }
        }
        Range.AfterShinkirou -> { // TODO: 小数点作
            (14..19).map { id -> "th%02d".format(id) }
        }
        Range.AllNew -> {
            (6..19).map { id -> "th%02d".format(id) }
        }
        Range.STGOnly -> {
            (1..19).map { id -> "th%02d".format(id) }
        }
        Range.FTGOnly -> {
            listOf()
        }
        Range.Old -> {
            (1..5).map { id -> "02d".format(id) }
        }
        Range.HifuuOnly -> {
            listOf()
        }
    }
    suspend fun start(event: GroupAtMessageEvent, difficulty: Difficulty, range: Range) = event.run event@{
        if (bot.restarting) {
            reply("bot即将重启，请等两三分钟后再试！")
            return@event
        }
        if (started[contextId] == true) {
            reply("当前群有正在进行的猜歌哦~")
            return@event
        }
        val time = getTime(difficulty)
        lateinit var name: String
        val answers = transactionWithLock {
            val selected = TouhouMusics.select {
                TouhouMusics.version inList rangeToList(range)
            }.toList().random()
            println(selected)
            name = selected[TouhouMusics.name]
            val id = selected[TouhouMusics.id].value
            val filename = selected[TouhouMusics.filename]
            val version = selected[TouhouMusics.version]
            var answers = TouhouAliases.select {
                TouhouAliases.id eq id
            }.map { it[TouhouAliases.alias] }.toMutableList()
            answers.add(name)
            answers = answers.map { it.trim().lowercase() }.toMutableList()
            val file = musicDir[filename]
            if (!file.cropPeriod(Random.nextDouble(0.0, file.getAudioDuration() - time), time).useTempFile {
                    reply(it.toVoice())
                }) {
                reply("出错了，请稍后重试")
                return@transactionWithLock null
            }
            reply("请回答该原曲的名称，两分钟后揭晓答案~\n(PS：作答时需要@可怜Bot哦)")
            return@transactionWithLock answers
        } ?: return@event
        println(answers)


        started[contextId] = true
        var finished = false
        coroutineScope {
            launch {
                GlobalEventChannel.channel.takeWhile { !finished }.collect { e ->
                    if (e is BotRestartEvent) {
                        finished = true
                        started[contextId] = false
                        reply("bot即将重启，已暂时终止游戏，为您带来不便深感抱歉。本轮原曲是$name")
                        return@collect
                    }
                    if (e !is PublicMessageEvent || e.contextId != contextId)
                        return@collect
                    launch {
                        val answer = e.message.text.trim().lowercase()
                        runCatching {
                            if (answers.any { answer.isNotBlank() && answer in it } ||
                                answers.any { answer.toPinyinAbbr().length >= 3 && it.toPinyinAbbr().length >= 3 && answer.toPinyinAbbr() in it.toPinyinAbbr() } ||
                                answers.any { it.filter { l -> l.code > 128 }.length >= 5 &&
                                        it.filter { l -> l.code > 128 }.toSet().intersect(
                                            answer.filter { l -> l.code > 128 }.toSet()
                                        ).size >= 3 }) {
                                e.reply("恭喜你猜中了！原曲是$name")
                                finished = true
                                started[contextId] = false
                            }
                        }
                    }
                }
            }
            launch {
                delay(120000L + (time * 1000L).toLong())
                if (finished)
                    return@launch
                finished = true
                started[contextId] = false
                reply("很遗憾，无人猜中，原曲是$name")
            }
        }
    }
}