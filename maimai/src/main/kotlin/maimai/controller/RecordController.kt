package xyz.xszq.bot.maimai.controller

import korlibs.math.toIntCeil
import xyz.xszq.bot.Maimai
import xyz.xszq.bot.maimai.component.MaimaiQuery
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.maimai.music.MusicDifficulty
import xyz.xszq.bot.maimai.music.UserQueryParams
import xyz.xszq.bot.maimai.query.ComboQuery
import xyz.xszq.bot.maimai.query.ComboQuery.filterCharts
import xyz.xszq.bot.maimai.query.ComboQuery.filterMusics
import xyz.xszq.bot.maimai.query.ComboQuery.filterRecords
import xyz.xszq.bot.reply

@Suppress("unused")
class RecordController(
    override val maimai: Maimai
): Controller(maimai) {
    override suspend fun setRoute() = rhythm {
        commandEndsWith("进度") { raw ->
            val args = raw.split(" ")
            val command = args.first()
            val queryArgs = args.getOrNull(1) ?: ""
            var user: UserQueryParams? = null
            runCatching {
                user = maimai.query.getQueryParams(this, queryArgs)
                handleProgress(this, command, user) ?.let { result ->
                    reply(result)
                } ?: reply(MaimaiQuery.NO_RECORDS)
            }.onFailure { e ->
                handleError(this, e, user)
            }
        }
    }

    suspend fun handleProgress(
        event: MessageEvent,
        fullCommand: String,
        user: UserQueryParams
    ): String? {
        val filters = ComboQuery.filters(fullCommand)
        val musics = filters.filterMusics(maimai.musics())
        val charts = filters.filterCharts(maimai.musics())
        val (response, _) = maimai.query.records(user, musics)
        val records = filters.filterRecords(response.records, true) ?: return null
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