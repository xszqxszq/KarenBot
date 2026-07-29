package xyz.xszq.bot.maimai.controller

import xyz.xszq.bot.ErrorHandler
import xyz.xszq.bot.Maimai
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.exception.ArgsNotEnoughException
import xyz.xszq.bot.exception.IllegalArgsException
import xyz.xszq.bot.exception.NeedHelpException
import xyz.xszq.bot.exception.NotFoundException
import xyz.xszq.bot.maimai.music.MusicDifficulty
import xyz.xszq.bot.newLine
import xyz.xszq.bot.reply

@Suppress("unused")
class CalcController(
    override val maimai: Maimai
): Controller(maimai) {
    override suspend fun setRoute() = rhythm {
        startsWith("分数线") { raw ->
            runCatching {
                calc(raw)
            }.onFailure { e ->
                calcErrorHandler(e)
            }
        }
    }
    suspend fun MessageEvent.calc(
        raw: String
    ) {
        val args = raw.split(" ").filter { it.isNotBlank() }
        if (args.isEmpty() || (args.size == 1 && args.first() == "帮助"))
            throw NeedHelpException()
        if (args.size < 2)
            throw ArgsNotEnoughException()

        val difficulty = MusicDifficulty.from(args[0].substring(0, 1)) ?: throw IllegalArgsException()
        val music = maimai.aliases.search(args[0].substring(1)).firstOrNull() ?: throw NotFoundException()
        if (music.charts.size <= difficulty.value)
            throw NotFoundException()

        val line = args[1].toDouble()
        val notes = music.charts.first { it.difficulty == difficulty }.notes
        val totalScore = notes.tap * 500.0 + notes.hold * 1000 + notes.slide * 1500 +
                notes.touch * 500 + notes.`break` * 2500
        val breakBonus = 0.01 / notes.`break`
        val break50Reduce = totalScore * breakBonus / 4
        val reduce = 101.0 - line
        reply(buildString {
            appendLine("[${difficulty.brief}] ${music.id}. ${music.name}")
            append("分数线 $line% 允许的最多 TAP GREAT 数量为 ")
            append(String.format("%.2f", totalScore * reduce / 10000))
            appendLine(" (每个 -" + String.format("%.4f", 10000.0 / totalScore) + "%),")
            append("BREAK 50落 (一共 ${notes.`break`} 个) 等价于 ")
            append(String.format("%.3f", break50Reduce / 100) + " 个 TAP GREAT ")
            append("(-" + String.format("%.4f", break50Reduce / totalScore * 100) + "%)")
        }.trim().newLine())
    }
    val calcErrorHandler: ErrorHandler = { e ->
        val help = buildString {
            appendLine("此功能为查找某首歌分数线设计。")
            appendLine("命令格式：分数线 <难度+歌曲id/名称/别名> <分数线>")
            appendLine("例如：分数线 紫379 100.5")
            appendLine("例如：分数线 白茄子 99.5")
            appendLine("命令将返回分数线允许的 TAP GREAT 容错以及 BREAK 50落等价的 TAP GREAT 数。")
            appendLine("以下为 TAP GREAT 的对应表：")
            appendLine("GREAT/GOOD/MISS")
            appendLine("TAP   1/2.5/5")
            appendLine("HOLD  2/5/10")
            appendLine("SLIDE 3/7/15")
            appendLine("TOUCH 1/2/5")
            appendLine("BREAK 5/12.5/25(外加200落)")
        }.trim().newLine()
        when (e) {
            is NeedHelpException -> reply(help)
            is ArgsNotEnoughException -> reply(help)
            is IllegalArgsException -> reply(help)
            is NotFoundException -> reply("未找到该歌曲或难度，请检查拼写。")
            else -> e.printStackTrace()
        }
    }
}