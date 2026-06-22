package xyz.xszq.bot.maimai.controller

import korlibs.math.toIntCeil
import xyz.xszq.bot.Maimai
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.maimai.component.MaimaiQuery
import xyz.xszq.bot.maimai.music.MusicDifficulty
import xyz.xszq.bot.maimai.music.MusicGenre
import xyz.xszq.bot.maimai.music.UserQueryParams
import xyz.xszq.bot.maimai.query.ComboQuery
import xyz.xszq.bot.maimai.query.ComboQuery.filterCharts
import xyz.xszq.bot.maimai.query.ComboQuery.filterMusics
import xyz.xszq.bot.maimai.query.ComboQuery.filterRecords
import xyz.xszq.bot.newLine
import xyz.xszq.bot.reply
import kotlin.math.sqrt

@Suppress("unused")
class RecordController(
    override val maimai: Maimai
): Controller(maimai) {
    override suspend fun setRoute() = rhythm {
        commandEndsWith("进度") { (command, queryArgs) ->
            var user: UserQueryParams? = null
            runCatching {
                user = maimai.query.getQueryParams(this, queryArgs ?: "")
                handleProgress(this, command, user) ?.let { result ->
                    reply(result)
                } ?: reply(MaimaiQuery.NO_RECORDS)
            }.onFailure { e ->
                handleError(this, e, user)
            }
        }
        startsWith(listOf("上分推荐", "吃分推荐")) { raw ->
//            val queryArgs = raw.ifBlank { "" }
//            var user: UserQueryParams? = null
//            runCatching {
//                user = maimai.query.getQueryParams(this, queryArgs)
//                handleRecommend(user)
//            }.onFailure { e ->
//                handleError(this, e, user)
//            }
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

    suspend fun MessageEvent.handleRecommend(
        user: UserQueryParams
    ) {
        val radarData = maimai.image.radar.data
        if (radarData.isEmpty()) {
            return
        }

        val (rating, _) = maimai.query.rating(user)
        val b50Records = rating.oldRatingList + rating.newRatingList
        if (b50Records.isEmpty()) {
            reply(MaimaiQuery.NO_RECORDS)
            return
        }

        val weightedRadar = doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0)
        var totalWeight = 0.0
        var baseRatingSum = 0.0
        val wellPlayedSet = mutableSetOf<Pair<Int, Int>>()

        b50Records.forEachIndexed { index, record ->
            val weight = 1.0 - (index / 49.0) * 0.5
            val musicId = record.chart.music.id.toString()
            val diffValue = record.chart.difficulty.value
            val radar = radarData[musicId]?.getOrNull(diffValue) ?: return@forEachIndexed

            weightedRadar[0] += radar.notes * weight
            weightedRadar[1] += radar.peak * weight
            weightedRadar[2] += radar.stamina * weight
            weightedRadar[3] += radar.slide * weight
            weightedRadar[4] += radar.handTrip * weight
            totalWeight += weight
            baseRatingSum += record.chart.levelValue * weight

            if (record.achievement >= 995000)
                wellPlayedSet.add(Pair(record.chart.music.id, diffValue))
        }

        if (totalWeight == 0.0) {
            return
        }

        val playerVector = doubleArrayOf(
            weightedRadar[0] / totalWeight,
            weightedRadar[1] / totalWeight,
            weightedRadar[2] / totalWeight,
            weightedRadar[3] / totalWeight,
            weightedRadar[4] / totalWeight
        )
        val baseRating = baseRatingSum / totalWeight

        val candidateRange = baseRating..(baseRating + 0.4)
        val allNonUtageMusics = maimai.musics().filter { it.genre != MusicGenre.Utage }
        val levelFilteredCharts = allNonUtageMusics.flatMap { music ->
            music.charts.filter { chart ->
                chart.difficulty != MusicDifficulty.Utage &&
                chart.levelValue in candidateRange &&
                Pair(music.id, chart.difficulty.value) !in wellPlayedSet &&
                radarData[music.id.toString()]?.getOrNull(chart.difficulty.value) != null
            }
        }

        val candidateMusics = levelFilteredCharts.map { it.music }.distinct()
        if (candidateMusics.isNotEmpty()) {
            val (records, _) = maimai.query.records(user, candidateMusics)
            records.records.forEach { record ->
                if (record.achievement >= 995000)
                    wellPlayedSet.add(Pair(record.chart.music.id, record.chart.difficulty.value))
            }
        }

        val candidates = levelFilteredCharts.filter { chart ->
            Pair(chart.music.id, chart.difficulty.value) !in wellPlayedSet
        }

        val scored = candidates.mapNotNull { chart ->
            val radar = radarData[chart.music.id.toString()]!![chart.difficulty.value]!!
            val chartVector = doubleArrayOf(
                radar.notes, radar.peak, radar.stamina, radar.slide, radar.handTrip
            )
            val sim = cosineSimilarity(playerVector, chartVector)
            if (sim > 0.85) Pair(chart, sim) else null
        }.sortedByDescending { it.second }
            .take(5)

        if (scored.isEmpty()) {
            reply("暂未找到合适的推荐谱面。")
            return
        }

        // 6. 构造回复
        val message = buildString {
            appendLine("推荐的吃分谱面：")
            appendLine()
            scored.forEachIndexed { index, (chart, _) ->
                val diffName = chart.difficulty.names.last()
                appendLine("${index + 1}. ${chart.music.id}. ${chart.music.name}")
                appendLine("   $diffName ${chart.level}（定数 ${chart.levelValue}）")
                appendLine()
            }
        }
        reply(message.newLine())
    }

    companion object {
        fun cosineSimilarity(v1: DoubleArray, v2: DoubleArray): Double {
            var dotProduct = 0.0
            var norm1 = 0.0
            var norm2 = 0.0
            for (i in 0 until 5.coerceAtMost(v1.size).coerceAtMost(v2.size)) {
                dotProduct += v1[i] * v2[i]
                norm1 += v1[i] * v1[i]
                norm2 += v2[i] * v2[i]
            }
            if (norm1 == 0.0 || norm2 == 0.0) return 0.0
            return dotProduct / (sqrt(norm1) * sqrt(norm2))
        }
    }
}