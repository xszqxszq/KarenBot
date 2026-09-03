package xyz.xszq.bot.audio.touhou

import com.github.houbb.opencc4j.util.ZhConverterUtil
import com.github.houbb.pinyin.constant.enums.PinyinStyleEnum
import com.github.houbb.pinyin.util.PinyinHelper
import korlibs.io.file.std.localCurrentDirVfs
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import org.xm.Similarity
import xyz.xszq.bot.AudioHandler.crop
import xyz.xszq.bot.AudioHandler.duration
import xyz.xszq.bot.ErrorHandler
import xyz.xszq.bot.Plugin
import xyz.xszq.bot.event.GroupMessageEvent
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.exception.IllegalArgsException
import xyz.xszq.bot.exception.NeedHelpException
import xyz.xszq.bot.message.Audio
import xyz.xszq.bot.message.Markdown
import xyz.xszq.bot.payload.markdown.Keyboard
import xyz.xszq.bot.payload.markdown.RenderData
import xyz.xszq.bot.reply
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * 东方原曲测验
 *
 * 随机截取东方原曲片段并让玩家猜出原曲名/所属原作及出现位置，原曲位于 data/audio/touhou
 */
class Touhou(
    val audio: Plugin
) {
    val baseDir = localCurrentDirVfs["data/audio/touhou"]
    lateinit var musics: TouhouMusics
    private val started = ConcurrentHashMap<String, Boolean>()

    suspend fun init() {
        musics = Json.decodeFromString(baseDir["musics.json"].readString())
    }

    /**
     * 注册猜歌相关路由
     */
    suspend fun setRoute() = audio.route {
        // 获取一首随机东方原曲
        startsWith("随机东方原曲") {
            val (game, target) = musics.categories.flatMap { category ->
                category.games.flatMap { game ->
                    game.tracks.map { track ->
                        Pair(game, track)
                    }
                }
            }.random(
                Random(System.currentTimeMillis())
            )
            val file = baseDir[target.file]
            val duration = file.duration() ?: run {
                return@startsWith
            }
            if (duration <= RANDOM_DURATION)
                return@startsWith
            val offset = Random(System.currentTimeMillis())
                .nextDouble(0.0, duration - RANDOM_DURATION)
            file.crop(offset, RANDOM_DURATION) { cropped ->
                reply(Audio(cropped))
                reply(Markdown.create {
                    line(bold("随机东方原曲"))
                    line()
                    line("${target.name}")
                    line("来自${game.id}. ${game.name}")
                    keyboard {
                        row {
                            at(
                                label = "再来一抽",
                                data = "随机东方原曲",
                                enter = true,
                                style = RenderData.FILLED_BLUE,
                                id = "1"
                            )
                        }
                    }
                })
            }
        }
        // 启动原曲认知测验
        startsWith(listOf("原曲认知测验", "猜东方原曲")) { raw ->
            runCatching {
                guess(raw)
            }.onFailure { e ->
                guessErrorHandler(e)
            }
        }
        // 终止猜歌并清理
        startsWith("不玩了") {
            val id = if (this is GroupMessageEvent) group.id else sender.id
            if (started.containsKey(id))
                started.remove(id)
        }
    }
    suspend fun MessageEvent.guess(
        raw: String
    ) {
        val args = raw.trim().split(" ").filter { it.isNotBlank() }
        if (args.isEmpty()) {
            throw NeedHelpException()
        }
        val difficulty = when (args[0].lowercase()) {
            in listOf("easy", "e") -> Difficulty.Easy
            in listOf("normal", "n") -> Difficulty.Normal
            in listOf("hard", "h") -> Difficulty.Hard
            in listOf("lunatic", "l") -> Difficulty.Lunatic
            else -> throw IllegalArgsException("该难度不存在！")
        }
        val range = when {
            args.size < 2 -> defaultRange
            args[1] == "新作" -> Range.New
            args[1] == "旧作" -> Range.Old
            args[1] == "全部" -> Range.Int
            else -> throw IllegalArgsException("该题目范围不存在！")
        }

        if (!playable())
            return
        val id = if (this is GroupMessageEvent) group.id else sender.id

        val (game, music) = musics.categories.flatMap { category ->
            category.games.filter {
                when (range) {
                    Range.New -> category.name == "整数新作"
                    Range.Old -> category.name == "旧作"
                    Range.Int -> category.name == "整数新作" ||
                        category.name == "旧作"
                }
            }.flatMap { game ->
                game.tracks.map { Pair(game.name, it) }
            }
        }.random(Random(System.currentTimeMillis()))
        audio.logger.debug { "题目曲目：${music.name}" }

        val musicFile = baseDir[music.file]
        val duration = musicFile.duration() ?: run {
            started.remove(id)
            return
        }
        val slice = difficulty.duration()
        if (duration <= slice) {
            started.remove(id)
            return
        }
        val offset = Random(System.currentTimeMillis())
            .nextDouble(0.0, duration - slice)
        musicFile.crop(offset, slice) { cropped ->
            var answers = music.aliases.toMutableList()
            answers.filter { "～" in it }.forEach { before ->
                before.split("～").map { it.trim() }.forEach {
                    answers.add(it)
                }
            }
            val gamePrefix = game.substringAfter("东方")
            val gameSimplified = gamePrefix
                .map { it.toString() }
                .map { PinyinHelper.toPinyin(it, PinyinStyleEnum.NORMAL).first() }
                .joinToString("")
            answers.filter { true }.forEach { before ->
                answers.add(game + before)
                answers.add(gamePrefix + before)
                answers.add(gameSimplified + before)
            }
            answers.filter {
                "面道中" in it || "面boss" in it || "面主题曲" in it
            }.forEach { before ->
                val levelName = before
                    .replace("面道中", "面")
                    .replace("面boss", "面")
                    .replace("面主题曲", "面")
                answers.add(game + levelName)
                answers.add(gamePrefix + levelName)
                answers.add(gameSimplified + levelName)
            }
            answers = answers
                .map { it.toSimple().lowercase() }
                .toSet()
                .toMutableList()
            audio.logger.debug { "可接受答案：$answers" }
            var finished = false

            reply(Audio(cropped))
            reply(Markdown.create {
                line(bold("原曲认知测验"))
                line()
                line("请回答该原曲的名称，一分钟后揭晓答案~")
                keyboard {
                    row {
                        at(
                            label = "回答",
                            data = " ",
                            id = ""
                        )
                        at(
                            label = "不玩了",
                            data = "不玩了",
                            enter = true,
                            style = RenderData.GRAY,
                            id = ""
                        )
                    }
                }
            })

            val subscribeId = UUID.randomUUID().toString()
            bot.pluginLoader.subscribes.always(subscribeId) {

                val nowId =
                    if (this is GroupMessageEvent) group.id else sender.id
                if (id != nowId) {
                    return@always
                }
                if (text.trim().startsWith("不玩了")) {
                    finished = true
                    reply(Markdown.create {
                        line(bold("原曲认知测验"))
                        line()
                        line("游戏已结束。答案是${music.answer()}")
                        keyboard(againKeyboard(difficulty, range))
                    })
                    bot.pluginLoader.subscribes.stop(subscribeId)
                    return@always
                }

                if (answers.isAnswer(text.trim())) {
                    finished = true
                    reply(Markdown.create {
                        line(bold("原曲认知测验"))
                        line()
                        line("恭喜你猜中了哦~答案是${music.answer()}")
                        keyboard(againKeyboard(difficulty, range))
                    })
                    bot.pluginLoader.subscribes.stop(subscribeId)
                    started.remove(id)
                    return@always
                }
            }

            delay(TIMESUP)
            if (finished)
                return@guess
            reply(Markdown.create {
                line(bold("原曲认知测验"))
                line()
                line("很遗憾，没有人猜中哦，答案是${music.answer()}")
                keyboard(againKeyboard(difficulty, range))
            })
            bot.pluginLoader.subscribes.stop(subscribeId)
            started.remove(id)
        }
    }
    fun againKeyboard(
        difficulty: Difficulty,
        range: Range
    ) = Keyboard.create {
        row {
            val base = "/原曲认知测验 ${difficulty.name}"
            val command = if (range != defaultRange)
                "$base ${range.value}"
            else
                base
            at(
                label = "再来一局",
                data = command,
                id = ""
            )
        }
    }

    fun isSimilar(
        a: String,
        b: String
    ): Boolean {
        return Similarity.cilinSimilarity(a, b) > SIMILAR_THRESHOLD ||
            Similarity.pinyinSimilarity(a, b) > SIMILAR_THRESHOLD ||
            Similarity.charBasedSimilarity(a, b) > SIMILAR_THRESHOLD ||
            Similarity.editDistanceSimilarity(a, b) > SIMILAR_THRESHOLD ||
            Similarity.standardEditDistanceSimilarity(a, b) > SIMILAR_THRESHOLD ||
            Similarity.gregorEditDistanceSimilarity(a, b) > SIMILAR_THRESHOLD
    }
    fun List<String>.isAnswer(reply: String): Boolean {
        val answer = reply.lowercase().toSimple().trim()
        val matched = any {
            ((it.length < 3 || answer.length >= 3) && answer in it) ||
                it in answer
        }
        if (answer in this || matched)
            return true
        return any { isSimilar(it, answer) }
    }
    fun Music.answer(): String {
        if (name == jpn)
            return name
        return "$name ($jpn)"
    }
    private suspend fun MessageEvent.playable(): Boolean {
        val id =
            if (this is GroupMessageEvent) group.id else sender.id
        if (started.containsKey(id)) {
            reply("当前还有猜题游戏正在进行中，回复机器人“不玩了”结束游戏")
            return false
        }
        started[id] = true
        return true
    }
    val guessErrorHandler: ErrorHandler = { e ->
        when (e) {
            is NeedHelpException ->
                reply(Markdown.create {
                    line(bold("原曲认知测验"))
                    line()
                    line("请选择要进行的难度和模式：")
                    keyboard {
                        row {
                            Difficulty.entries.forEach { difficulty ->
                                at(
                                    label = difficulty.name,
                                    data = "/原曲认知测验 " +
                                        difficulty.name.lowercase(),
                                    enter = true,
                                    id = ""
                                )
                            }
                        }
                        row {
                            at(
                                label = "猜新作",
                                data = "/原曲认知测验 normal 新作",
                                enter = true,
                                id = ""
                            )
                            at(
                                label = "猜旧作",
                                data = "/原曲认知测验 normal 旧作",
                                enter = true,
                                id = ""
                            )
                            at(
                                label = "猜全部",
                                data = "/原曲认知测验 normal 全部",
                                enter = true,
                                id = ""
                            )
                        }
                    }
                })
            is IllegalArgsException -> reply(e.message ?: "")
            else -> e.printStackTrace()
        }
    }
    companion object {
        const val RANDOM_DURATION = 15.0
        const val TIMESUP = 60000L
        const val SIMILAR_THRESHOLD = 0.7
        enum class Difficulty {
            Easy, Normal, Hard, Lunatic
        }
        enum class Range(val value: String) {
            Old("旧作"), New("新作"), Int("全部")
        }
        val defaultRange = Range.New
        private fun Difficulty.duration() = when (this) {
            Difficulty.Easy -> 10.0
            Difficulty.Normal -> 5.0
            Difficulty.Hard -> 2.0
            Difficulty.Lunatic -> 1.0
            // TODO: 支持 Extra 难度
        }
        fun String.toSimple() = ZhConverterUtil.toSimple(this) ?: this
    }
}