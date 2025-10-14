package xyz.xszq.bot.controller

import korlibs.math.toIntCeil
import xyz.xszq.bot.Maimai
import xyz.xszq.bot.Query
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.music.MusicDifficulty
import xyz.xszq.bot.reply

@Suppress("unused")
class RecordController(
    override val maimai: Maimai
): Controller(maimai) {

    override fun setRoute() = maimai.route("/mai") {
        commandEndsWith("进度") { raw ->
            val args = raw.split(" ")
            val command = args.first()
            val arg = args.getOrNull(1) ?: ""
            handleProgress(this, command, arg) ?.let { result ->
                reply(result)
            } ?: reply(maimai.query.noRecords)
        }
    }

    suspend fun handleProgress(
        event: MessageEvent,
        fullCommand: String,
        args: String
    ): String? {
        val filters = Query.filters(fullCommand)
        val musics = Query.filterMusics(filters, maimai.musics())
        val charts = Query.filterCharts(filters, maimai.musics())
        val response = maimai.query.records(event, musics) ?: return null
        val records = Query.filterRecords(filters, response.records, true) ?: return null
        if (records.size == charts.size)
            return "您已经达成了${fullCommand}的条件。"
        return buildString {
            appendLine("您的${fullCommand}进度如下：")
            var totalRemains = 0
            MusicDifficulty.entries.forEach { difficulty ->
                val difficultyName = difficulty.names.first { it.contains("谱") }
                val total = charts.count { it.difficulty == difficulty }
                val completed = records.count { it.chart.difficulty == difficulty }
                val remains = total - completed
                if (remains > 0) {
                    appendLine("${difficultyName}余${remains}个 (共${total}个)")
                    totalRemains += remains
                }
            }

            append("总计${totalRemains}个")
            val (hours, minutes) = if ("舞舞" !in fullCommand) {
                val pcs = (totalRemains / 3.0).toIntCeil()
                val time = pcs * 10
                append("，单刷需${pcs}pc，即")
                Pair(time / 60, time % 60)
            } else {
                val pcs = (totalRemains / 4.0).toIntCeil()
                val time = pcs * 15
                append("，拼机需${pcs}pc，即")
                Pair(time / 60, time % 60)
            }
            if (hours > 0)
                append("${hours}小时")
            if (minutes > 0)
                append("${minutes}分钟")
        }.trim()
    }
}